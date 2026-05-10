from pydantic import BaseModel


class QuoteCreate(BaseModel):
    text: str
    source: str = ""


class QuoteResponse(BaseModel):
    id: int
    text: str
    source: str
    created_at: str


class QuoteListResponse(BaseModel):
    total: int
    quotes: list[QuoteResponse]


class ScheduleCreate(BaseModel):
    title: str
    date: str = ""
    time: str = ""


class ScheduleToggle(BaseModel):
    done: int
