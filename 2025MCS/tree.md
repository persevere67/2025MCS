<pre id="tree-panel"><bold>src</bold><br/> ┣ assets<br/> ┃ ┣ bj1.jpg<br/> ┃ ┣ bj2.jpg<br/> ┃ ┣ bj3.jpg<br/> ┃ ┣ bj4.jpg<br/> ┃ ┣ logo.png<br/> ┃ ┗ vue.svg<br/> ┣ components<br/> ┃ ┣ admin<br/> ┃ ┃ ┗ AdminPage.vue<br/> ┃ ┣ auth<br/> ┃ ┃ ┣ AuthContainer.vue<br/> ┃ ┃ ┣ LoginForm.vue<br/> ┃ ┃ ┗ RegisterForm.vue<br/> ┃ ┣ common<br/> ┃ ┃ ┣ Footer.vue<br/> ┃ ┃ ┗ Header.vue<br/> ┃ ┣ user<br/> ┃ ┃ ┣ DrugDetailPage.vue<br/> ┃ ┃ ┣ HistoryPage.vue<br/> ┃ ┃ ┗ QAPage.vue<br/> ┃ ┗ HomePage.vue<br/> ┣ router<br/> ┃ ┗ index.js<br/> ┣ stores<br/> ┃ ┗ auth.js<br/> ┣ utils<br/> ┃ ┗ api.js<br/> ┣ App.vue<br/> ┗ main.js</pre>





`<pre id="tree-panel"><bold>`src `</bold><br/>` ┗ main `<br/>` ┃ ┣ java `<br/>` ┃ ┃ ┗ com `<br/>` ┃ ┃ ┃ ┗ medical `<br/>` ┃ ┃ ┃ ┃ ┗ qna `<br/>` ┃ ┃ ┃ ┃ ┃ ┗ medical_qna_system `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┣ common `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ enums `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ ErrorCode.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ UserRole.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┣ config `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ CorsConfig.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ ErrorConfig.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ PasswordConfig.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ SecurityConfig.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ WebClientConfig.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ WebMvcConfig.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┣ controller `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ AuthController.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ QuestionController.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┣ dto `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ request `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ LoginRequest.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ QuestionRequest.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ RegisterRequest.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ response `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ AnswerResponse.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ ApiResponse.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ DiseaseInfoDto.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ QuestionAnswerDto.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ SessionStatus.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ UserDto.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┣ entity `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ mysql `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ QuestionAnswer.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ User.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ neo4j `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ Complication.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ Department.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ DepartmentCategory.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ Disease.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ Food.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ Symptom.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ Treatment.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┣ exception `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ BusinessException.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ GlobalExceptionHandler.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┣ filter `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ JwtAuthenticationFilter.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┣ repository `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ mysql `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ QuestionAnswerRepository.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ UserRepository.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ neo4j `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ MedicalKnowledgeRepository.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┣ security `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ JwtTokenUtil.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ PasswordValidator.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ SessionManager.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┣ service `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ impl `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ AuthServiceImpl.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ QuestionServiceImpl.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ UserServiceImpl.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ AuthService.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ QuestionService.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ UserService.java `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┗ MedicalQnaSystemApplication.java `<br/>` ┃ ┣ resources `<br/>` ┃ ┃ ┣ static `<br/>` ┃ ┃ ┃ ┣ assets `<br/>` ┃ ┃ ┃ ┃ ┣ bj1-Dvq-jdWR.jpg `<br/>` ┃ ┃ ┃ ┃ ┣ bj2-CTQxEyB2.jpg `<br/>` ┃ ┃ ┃ ┃ ┣ bj3-CROkdpgf.jpg `<br/>` ┃ ┃ ┃ ┃ ┣ bj4-C5No2Tir.jpg `<br/>` ┃ ┃ ┃ ┃ ┣ index-74QkWGgl.css `<br/>` ┃ ┃ ┃ ┃ ┣ index-BZkLYn0g.js `<br/>` ┃ ┃ ┃ ┃ ┗ logo-LZSjzN-h.png `<br/>` ┃ ┃ ┃ ┣ index.html `<br/>` ┃ ┃ ┃ ┗ vite.svg `<br/>` ┃ ┃ ┣ application-dev.properties `<br/>` ┃ ┃ ┣ application-prod.properties `<br/>` ┃ ┃ ┗ application.properties `<br/>` ┃ ┗ test `<br/>` ┃ ┃ ┗ java `<br/>` ┃ ┃ ┃ ┗ com `<br/>` ┃ ┃ ┃ ┃ ┗ medical `<br/>` ┃ ┃ ┃ ┃ ┃ ┗ qna `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┗ medical_qna_system `<br/>` ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ MedicalQnaSystemApplicationTests.java `</pre>`







 prompt =f"""Act like 一位专业、富有同理心的医疗问答助手。你擅长将复杂的医学信息转化为患者易于理解的语言，并能够在保证科学严谨的前提下给予温暖和清晰的健康建议。

    你的目标是：回答用户提出的医学相关问题，所有回答必须基于提供的背景知识为核心依据，必要时可补充严谨、可靠的医学资料。

    请按以下步骤操作：步骤 1：阅读 “背景知识” 段落，提取关键信息，明确它所提供的医学内容或结论。步骤 2：阅读 “用户问题”，识别问题核心，例如：疾病解释、药物作用、副作用、处理建议等。步骤 3：仅根据背景知识内容，使用通俗、连贯的中文，结构清晰地回答用户的问题。可使用小标题、段落或要点列出答案，确保患者或非专业读者能够理解。

    步骤 4：如果背景知识内容不足以完整回答问题，请根据权威医学网站（如 PubMed、WHO、UpToDate、国家卫健委等）检索相关资料进行补充说明。必须保持严谨，不得猜测或编造。清楚标注哪些内容来自扩展查询。步骤 5：结尾以温和、关怀的语言总结核心建议，若问题涉及个体诊疗，请建议咨询专业医生进一步确认。请注意：不要输出背景知识本身，仅用它作为回答依据。整个回答应当逻辑自洽、信息详实、语气亲切、医学上严谨。Take a deep breath and work on this problem step-by-step.

{context}

[问题]

{query}


[回答]: """
 prompt =f"""Act like 一位专业、富有同理心的医疗问答助手。你擅长将复杂的医学信息转化为患者易于理解的语言，并能够在保证科学严谨的前提下给予温暖和清晰的健康建议。

    你的目标是：回答用户提出的医学相关问题，所有回答必须基于提供的背景知识为核心依据，必要时可补充严谨、可靠的医学资料。

    请按以下步骤操作：步骤 1：阅读 “背景知识” 段落，提取关键信息，明确它所提供的医学内容或结论。步骤 2：阅读 “用户问题”，识别问题核心，例如：疾病解释、药物作用、副作用、处理建议等。步骤 3：仅根据背景知识内容，使用通俗、连贯的中文，结构清晰地回答用户的问题。可使用小标题、段落或要点列出答案，确保患者或非专业读者能够理解。

    步骤 4：如果背景知识内容不足以完整回答问题，请根据权威医学网站（如 PubMed、WHO、UpToDate、国家卫健委等）检索相关资料进行补充说明。必须保持严谨，不得猜测或编造。清楚标注哪些内容来自扩展查询。步骤 5：结尾以温和、关怀的语言总结核心建议，若问题涉及个体诊疗，请建议咨询专业医生进一步确认。请注意：不要输出背景知识本身，仅用它作为回答依据。整个回答应当逻辑自洽、信息详实、语气亲切、医学上严谨。Take a deep breath and work on this problem step-by-step.

{context}

[问题]

{query}


[回答]: """
