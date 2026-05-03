from sqlalchemy import Column, String, Numeric, TIMESTAMP, text, ForeignKey, Index
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import DeclarativeBase, relationship


class Base(DeclarativeBase):
    pass


class User(Base):
    __tablename__ = "users"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"))
    apple_user_id = Column(String, unique=True, nullable=False)
    tier = Column(String, nullable=False, server_default="free")
    created_at = Column(TIMESTAMP(timezone=True), server_default=text("NOW()"))

    cards = relationship("CollectionCard", back_populates="user", cascade="all, delete-orphan")


class CollectionCard(Base):
    __tablename__ = "collection"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"))
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    card_sku = Column(String, nullable=False)
    name = Column(String, nullable=False)
    set_code = Column(String)
    set_number = Column(String)
    language = Column(String, server_default="english")
    market_price = Column(Numeric(10, 2))
    price_source = Column(String)
    scanned_at = Column(TIMESTAMP(timezone=True), nullable=False)
    deleted_at = Column(TIMESTAMP(timezone=True))
    created_at = Column(TIMESTAMP(timezone=True), server_default=text("NOW()"))

    user = relationship("User", back_populates="cards")


Index("ix_collection_user_active", CollectionCard.user_id,
      postgresql_where=CollectionCard.deleted_at.is_(None))
