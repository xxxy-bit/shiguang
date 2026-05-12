import sqlite3
import os

DB_PATH = os.path.join(os.path.dirname(__file__), "quotes.db")


def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    conn = get_db()
    conn.execute("""
        CREATE TABLE IF NOT EXISTS quotes (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            text TEXT NOT NULL,
            source TEXT DEFAULT '',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS schedules (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            date TEXT NOT NULL DEFAULT '',
            time TEXT NOT NULL DEFAULT '',
            done INTEGER NOT NULL DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    conn.commit()
    conn.close()


def add_quote(text: str, source: str = "") -> int:
    conn = get_db()
    cursor = conn.execute(
        "INSERT INTO quotes (text, source) VALUES (?, ?)",
        (text, source)
    )
    conn.commit()
    qid = cursor.lastrowid
    conn.close()
    return qid


def get_random_quote() -> dict | None:
    conn = get_db()
    row = conn.execute(
        "SELECT id, text, source, created_at FROM quotes ORDER BY RANDOM() LIMIT 1"
    ).fetchone()
    conn.close()
    if row:
        return dict(row)
    return None


def get_all_quotes(limit: int = 50, offset: int = 0) -> list[dict]:
    conn = get_db()
    rows = conn.execute(
        "SELECT id, text, source, created_at FROM quotes ORDER BY id DESC LIMIT ? OFFSET ?",
        (limit, offset)
    ).fetchall()
    conn.close()
    return [dict(r) for r in rows]


def get_quote_count() -> int:
    conn = get_db()
    row = conn.execute("SELECT COUNT(*) as count FROM quotes").fetchone()
    conn.close()
    return row["count"]


def delete_quote(qid: int) -> bool:
    conn = get_db()
    cursor = conn.execute("DELETE FROM quotes WHERE id = ?", (qid,))
    conn.commit()
    deleted = cursor.rowcount > 0
    conn.close()
    return deleted


def update_quote(qid: int, text: str) -> bool:
    conn = get_db()
    conn.execute("UPDATE quotes SET text = ? WHERE id = ?", (text, qid))
    conn.commit()
    conn.close()
    return True


# ====== Schedule Operations ======

def add_schedule(title: str, date: str = "", time: str = "") -> int:
    conn = get_db()
    cursor = conn.execute(
        "INSERT INTO schedules (title, date, time) VALUES (?, ?, ?)",
        (title, date, time)
    )
    conn.commit()
    sid = cursor.lastrowid
    conn.close()
    return sid


def get_schedules() -> list[dict]:
    conn = get_db()
    rows = conn.execute(
        "SELECT id, title, date, time, done, created_at FROM schedules ORDER BY date ASC, time ASC"
    ).fetchall()
    conn.close()
    return [dict(r) for r in rows]


def toggle_schedule(sid: int, done: int) -> bool:
    conn = get_db()
    conn.execute("UPDATE schedules SET done = ? WHERE id = ?", (done, sid))
    conn.commit()
    conn.close()
    return True


def delete_schedule(sid: int) -> bool:
    conn = get_db()
    cursor = conn.execute("DELETE FROM schedules WHERE id = ?", (sid,))
    conn.commit()
    deleted = cursor.rowcount > 0
    conn.close()
    return deleted
