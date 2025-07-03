# final_train.py (Ultimate Safe Mode with Gradient Accumulation)

import json
import torch
from torch.utils.data import DataLoader
from sentence_transformers import SentenceTransformer, InputExample, losses, SentencesDataset, util
from transformers import get_linear_schedule_with_warmup
from torch.cuda.amp import GradScaler
import math
import os
import random
from tqdm import tqdm
import time

# --- 1. 配置区 ---
script_dir = os.path.dirname(os.path.abspath(__file__))
project_root = os.path.dirname(script_dir)
print(f"Project Root detected: {project_root}")

os.environ["TOKENIZERS_PARALLELISM"] = "false"
torch.set_float32_matmul_precision('high')

CONFIG = {
    "training_data_path": os.path.join(project_root, "data", "training_qa_pairs_final.json"),
    "base_model_path": os.path.join(project_root, "sbert-base-chinese-nli-local"),
    "output_model_path": os.path.join(project_root, "output", f"final-medical-sbert-A800-stable-{int(time.time())}"),
    "device": "cuda:1",

    # 【OOM 终极修正】
    # 1. 设置一个非常安全的物理批次大小
    "batch_size": 64,
    # 2. 设置梯度累积步数，以达到理想的有效批次大小 (64 * 8 = 512)
    "accumulation_steps": 8,

    "num_epochs": 3,
    "learning_rate": 2e-5,
}

# --- 2. 主训练流程 ---
if __name__ == "__main__":
    print(f"PyTorch Version: {torch.__version__}")

    if "cuda" in CONFIG["device"] and torch.cuda.is_available():
        device = torch.device(CONFIG["device"])
        device_type = 'cuda'
        print(f"Using Ultimate Safe Mode on device: {device}")
    else:
        device = torch.device("cpu")
        device_type = 'cpu'
        print(f"CUDA not available. Using CPU.")

    print(f"Physical Batch Size: {CONFIG['batch_size']}")
    print(f"Gradient Accumulation Steps: {CONFIG['accumulation_steps']}")
    print(f"Effective Batch Size: {CONFIG['batch_size'] * CONFIG['accumulation_steps']}")

    print(f"正在从本地加载基础模型: {CONFIG['base_model_path']}")
    model = SentenceTransformer(CONFIG["base_model_path"])
    model.to(device)

    print("torch.compile() is disabled for this safe-mode run.")

    print(f"正在加载训练数据: {CONFIG['training_data_path']}")
    with open(CONFIG["training_data_path"], 'r', encoding='utf-8') as f:
        qa_pairs = json.load(f)

    train_examples = []
    for pair in tqdm(qa_pairs, desc="转换数据为InputExample"):
        if len(pair) == 2 and isinstance(pair[0], str) and isinstance(pair[1], str):
            train_examples.append(InputExample(texts=[pair[0], pair[1]]))

    print(f"数据加载和转换完成，共 {len(train_examples)} 个训练样本。")

    train_dataset = SentencesDataset(train_examples, model)
    collate_fn = model.smart_batching_collate
    train_dataloader = DataLoader(
        train_dataset,
        shuffle=True,
        batch_size=CONFIG["batch_size"],
        collate_fn=collate_fn,
        num_workers=0,  # 保持为0以确保稳定
        pin_memory=False
    )
    train_loss = losses.MultipleNegativesRankingLoss(model)

    optimizer = torch.optim.AdamW(model.parameters(), lr=CONFIG["learning_rate"])
    total_training_steps = len(train_dataloader) * CONFIG["num_epochs"]
    warmup_steps = math.ceil(total_training_steps * 0.1)
    scheduler = get_linear_schedule_with_warmup(optimizer, num_warmup_steps=warmup_steps,
                                                num_training_steps=total_training_steps)
    scaler = GradScaler()

    os.makedirs(CONFIG["output_model_path"], exist_ok=True)
    print(f"训练好的新模型将被保存到: {CONFIG['output_model_path']}")
    print(f"Warmup steps: {warmup_steps}")

    print("开始最终的模型微调 (安全模式 + 梯度累积)...")
    model.train()

    for epoch in range(CONFIG["num_epochs"]):
        print(f"\n--- Epoch {epoch + 1}/{CONFIG['num_epochs']} ---")
        progress_bar = tqdm(train_dataloader, desc="Training", leave=True)

        optimizer.zero_grad()

        for i, (features, labels) in enumerate(progress_bar):
            features = list(map(lambda batch: util.batch_to_device(batch, device), features))
            labels = labels.to(device)

            with torch.amp.autocast(device_type=device_type, dtype=torch.float16):
                loss_value = train_loss(features, labels)
                loss_value = loss_value / CONFIG["accumulation_steps"]

            scaler.scale(loss_value).backward()

            if (i + 1) % CONFIG["accumulation_steps"] == 0:
                scaler.step(optimizer)
                scaler.update()
                scheduler.step()
                optimizer.zero_grad()

            progress_bar.set_postfix({'loss': loss_value.item() * CONFIG["accumulation_steps"]})

        epoch_save_path = os.path.join(CONFIG["output_model_path"], f"epoch_{epoch + 1}")
        print(f"Epoch {epoch + 1} finished. Saving model to {epoch_save_path}")
        if hasattr(model, '_orig_mod'):
            model._orig_mod.save(epoch_save_path)
        else:
            model.save(epoch_save_path)

    print("\n--- 恭喜！最终模型训练完成！ ---")