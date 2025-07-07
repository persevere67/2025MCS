uvicorn main2:app --reload --port 8000

测试：

```
curl -X POST http://localhost:8000/ask -H "Content-Type: application/json" -d "{\"question\": \"什么是糖尿病？\"}"
```
