import os
import uvicorn
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from fastapi.responses import HTMLResponse, PlainTextResponse

from database import init_db, add_quote, get_random_quote, get_all_quotes, get_quote_count, delete_quote, update_quote
from database import add_schedule, get_schedules, toggle_schedule, delete_schedule
from models import QuoteCreate, ScheduleCreate, ScheduleToggle

load_dotenv()

app = FastAPI(title="拾光")

__all__ = ["app"]


@app.on_event("startup")
def startup():
    init_db()
    os.makedirs("static", exist_ok=True)


# ====== 句子 CRUD API ======

@app.post("/quotes")
def create_quote(q: QuoteCreate):
    qid = add_quote(q.text, q.source)
    return {"id": qid, "msg": "ok"}


@app.get("/quotes/random")
def random_quote():
    quote = get_random_quote()
    if not quote:
        raise HTTPException(status_code=404, detail="还没有句子，快发一条给飞书Bot吧")
    return quote


@app.get("/quotes/raw")
def random_quote_text():
    quote = get_random_quote()
    if not quote:
        return PlainTextResponse("还没有句子", status_code=404)
    return PlainTextResponse(quote["text"], media_type="text/plain; charset=utf-8")


@app.get("/quotes")
def list_quotes(limit: int = 50, offset: int = 0):
    total = get_quote_count()
    quotes = get_all_quotes(limit, offset)
    return {"total": total, "quotes": quotes}


@app.delete("/quotes/{qid}")
def remove_quote(qid: int):
    if not delete_quote(qid):
        raise HTTPException(status_code=404, detail="句子不存在")
    return {"msg": "deleted"}


@app.put("/quotes/{qid}")
def edit_quote(qid: int, q: QuoteCreate):
    update_quote(qid, q.text)
    return {"msg": "ok"}


# ====== 日程 CRUD API ======

@app.post("/schedules")
def create_schedule(s: ScheduleCreate):
    sid = add_schedule(s.title, s.date, s.time)
    return {"id": sid, "msg": "ok"}


@app.get("/schedules")
def list_schedules():
    return {"schedules": get_schedules()}


@app.put("/schedules/{sid}")
def update_schedule(sid: int, t: ScheduleToggle):
    toggle_schedule(sid, t.done)
    return {"msg": "ok"}


@app.delete("/schedules/{sid}")
def remove_schedule(sid: int):
    if not delete_schedule(sid):
        raise HTTPException(status_code=404)
    return {"msg": "deleted"}


# ====== 管理页面 ======

@app.get("/", response_class=HTMLResponse)
def index():
    return """<!DOCTYPE html>
<html lang="zh">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>句子管理</title>
<style>
  *{margin:0;padding:0;box-sizing:border-box}
  body{font-family:-apple-system,BlinkMacSystemFont,sans-serif;background:#f5f5f5;padding:16px}
  h1{font-size:20px;margin-bottom:16px;color:#333}
  .card{background:#fff;border-radius:12px;padding:16px;margin-bottom:12px;box-shadow:0 1px 3px rgba(0,0,0,.1);display:flex;justify-content:space-between;align-items:flex-start}
  .card .text{flex:1;font-size:15px;line-height:1.6;color:#222;word-break:break-word}
  .card .meta{font-size:11px;color:#999;margin-top:6px}
  .card .del{background:none;border:none;color:#e74c3c;font-size:20px;cursor:pointer;padding:0 0 0 12px;line-height:1}
  .empty{text-align:center;color:#999;padding:60px 20px;font-size:15px}
  .stats{font-size:13px;color:#666;margin-bottom:16px}
  .add-form{background:#fff;border-radius:12px;padding:16px;margin-bottom:20px;box-shadow:0 1px 3px rgba(0,0,0,.1)}
  .add-form textarea{width:100%;border:1px solid #ddd;border-radius:8px;padding:10px;font-size:14px;resize:vertical;min-height:60px;font-family:inherit}
  .add-form button{background:#1a73e8;color:#fff;border:none;padding:8px 20px;border-radius:8px;font-size:14px;margin-top:8px;cursor:pointer}
</style>
</head>
<body>
<h1>句子收藏</h1>
<div class="stats" id="stats"></div>
<div class="add-form">
  <textarea id="newText" placeholder="输入句子..."></textarea>
  <button onclick="addQuote()">保存</button>
</div>
<div id="list"></div>
<script>
async function load(){
  const r=await fetch('/quotes');
  const d=await r.json();
  document.getElementById('stats').textContent=`共 ${d.total} 条`;
  const el=document.getElementById('list');
  if(!d.quotes.length){el.innerHTML='<div class="empty">还没有句子，发给飞书Bot吧</div>';return}
  el.innerHTML=d.quotes.map(q=>`<div class="card"><div><div class="text">${q.text.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')}</div><div class="meta">#${q.id} · ${q.created_at}</div></div><button class="del" onclick="del(${q.id})">&times;</button></div>`).join('');
}
async function addQuote(){
  const t=document.getElementById('newText').value.trim();
  if(!t)return;
  await fetch('/quotes',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({text:t})});
  document.getElementById('newText').value='';
  load();
}
async function del(id){
  await fetch('/quotes/'+id,{method:'DELETE'});
  load();
}
load();
</script>
</body>
</html>"""


if __name__ == "__main__":
    import sys
    dev_mode = "--dev" in sys.argv
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=dev_mode)
