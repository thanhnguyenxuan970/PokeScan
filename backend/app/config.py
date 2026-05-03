from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env")

    tcgplayer_public_key: str = ""
    tcgplayer_private_key: str = ""
    environment: str = "development"


settings = Settings()
