import json
import requests
import os
from database import add_quote

APP_ID = os.getenv("FEISHU_APP_ID", "")
APP_SECRET = os.getenv("FEISHU_APP_SECRET", "")

_token_cache = {"token": None}


def get_tenant_access_token() -> str:
    if _token_cache["token"]:
        return _token_cache["token"]

    resp = requests.post(
        "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal",
        json={"app_id": APP_ID, "app_secret": APP_SECRET},
        timeout=10
    )
    data = resp.json()
    if data.get("code") == 0:
        _token_cache["token"] = data["tenant_access_token"]
        return _token_cache["token"]
    raise Exception(f"获取飞书Token失败: {data.get('msg')}")


def reply_message(message_id: str, content: str):
    token = get_tenant_access_token()
    resp = requests.post(
        f"https://open.feishu.cn/open-apis/im/v1/messages/{message_id}/reply",
        headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json"},
        json={
            "content": json.dumps({"text": content}),
            "msg_type": "text"
        },
        timeout=10
    )
    return resp.json()


def handle_message_event(event: dict) -> dict:
    message = event.get("message", {})
    msg_type = message.get("message_type", "")
    content_str = message.get("content", "{}")

    try:
        content = json.loads(content_str)
    except json.JSONDecodeError:
        return {"code": 0, "msg": "parse error"}

    text = content.get("text", "").strip()
    if not text:
        return {"code": 0, "msg": "empty"}

    qid = add_quote(text, source="飞书Bot")
    reply_message(message.get("message_id", ""), f"已记录 #{qid}")
    return {"code": 0, "msg": "ok", "quote_id": qid}
