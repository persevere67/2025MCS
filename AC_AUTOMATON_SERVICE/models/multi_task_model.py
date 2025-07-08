import torch
from torch import nn
from transformers import AutoModel, PreTrainedModel, AutoConfig
from transformers.modeling_outputs import TokenClassifierOutput, SequenceClassifierOutput

class MultiTaskMedicalQAModel(PreTrainedModel):
    # 使用PreTrainedModel以便于保存和加载
    config_class = AutoConfig
    base_model_prefix = "encoder"

    def __init__(self, config):
        super().__init__(config)
        self.config = config
        self.encoder = AutoModel.from_pretrained(config.model_name, config=config)
        self.dropout = nn.Dropout(config.hidden_dropout_prob)

        # NER head
        self.ner_classifier = nn.Linear(config.hidden_size, config.num_ner_labels)
        self.ner_loss_fct = nn.CrossEntropyLoss(ignore_index=-100) # -100 is ignored for padding

        # Intent Recognition head
        self.intent_classifier = nn.Linear(config.hidden_size, config.num_intent_labels)
        self.intent_loss_fct = nn.CrossEntropyLoss()

        # Sentence Embedding for Question Association (no extra head, uses pooled_output)
        # The quality of sentence_embedding depends on overall training and loss function design.
        # For simplicity in this example, we directly use pooled_output.
        # In a full SBERT setup, you'd add contrastive loss for positive/negative pairs.

        self.init_weights() # Initialize weights for the new layers

    # Override _init_weights if needed, or rely on transformers' default
    def _init_weights(self, module):
        """Initialize the weights"""
        if isinstance(module, nn.Linear):
            module.weight.data.normal_(mean=0.0, std=self.config.initializer_range)
            if module.bias is not None:
                module.bias.data.zero_()
        elif isinstance(module, nn.Embedding):
            module.weight.data.normal_(mean=0.0, std=self.config.initializer_range)
            if module.padding_idx is not None:
                module.weight.data[module.padding_idx].zero_()
        elif isinstance(module, nn.LayerNorm):
            module.bias.data.zero_()
            module.weight.data.fill_(1.0)


    def forward(self,
                input_ids=None,
                attention_mask=None,
                token_type_ids=None,
                ner_labels=None,
                intent_labels=None,
                # For SBERT-like contrastive learning, you'd also pass paired inputs
                # paired_input_ids=None,
                # paired_attention_mask=None,
                # paired_token_type_ids=None,
                # is_positive_pair=None # Boolean indicating if it's a positive pair for contrastive loss
                ):
        outputs = self.encoder(
            input_ids=input_ids,
            attention_mask=attention_mask,
            token_type_ids=token_type_ids,
            return_dict=True
        )

        sequence_output = outputs.last_hidden_state  # [batch_size, sequence_length, hidden_size]
        # Use mean pooling for sentence embedding for simplicity
        # More advanced SBERT uses CLS token or smart pooling
        # For a simple approach, mean pooling over non-padded tokens is common for sentence embeddings
        input_mask_expanded = attention_mask.unsqueeze(-1).expand(sequence_output.size()).float()
        sum_embeddings = torch.sum(sequence_output * input_mask_expanded, 1)
        sum_mask = torch.clamp(input_mask_expanded.sum(1), min=1e-9)
        pooled_output = sum_embeddings / sum_mask # [batch_size, hidden_size]

        # Apply dropout
        sequence_output = self.dropout(sequence_output)
        pooled_output = self.dropout(pooled_output)

        # --- NER Loss ---
        ner_logits = self.ner_classifier(sequence_output)
        ner_loss = None
        if ner_labels is not None:
            # Only consider active parts of the sequence for NER loss
            if attention_mask is not None:
                active_loss = attention_mask.view(-1) == 1
                active_logits = ner_logits.view(-1, self.config.num_ner_labels)
                active_labels = torch.where(
                    active_loss, ner_labels.view(-1), torch.tensor(self.ner_loss_fct.ignore_index).type_as(ner_labels)
                )
                ner_loss = self.ner_loss_fct(active_logits, active_labels)
            else:
                ner_loss = self.ner_loss_fct(ner_logits.view(-1, self.config.num_ner_labels), ner_labels.view(-1))

        # --- Intent Loss ---
        intent_logits = self.intent_classifier(pooled_output)
        intent_loss = None
        if intent_labels is not None:
            intent_loss = self.intent_loss_fct(intent_logits, intent_labels)

        # --- Combined Loss ---
        total_loss = None
        if ner_loss is not None and intent_loss is not None:
            # You can weigh the losses if one task is more important
            total_loss = ner_loss + intent_loss
        elif ner_loss is not None:
            total_loss = ner_loss
        elif intent_loss is not None:
            total_loss = intent_loss

        return {
            "loss": total_loss,
            "ner_logits": ner_logits,
            "intent_logits": intent_logits,
            "sentence_embedding": pooled_output, # For question association (retrieval)
            # You could add paired_sentence_embedding and contrastive_loss here if implementing SBERT loss
        }