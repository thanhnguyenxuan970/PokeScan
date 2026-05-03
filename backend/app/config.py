from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env")

    tcgplayer_public_key: str = ""
    tcgplayer_private_key: str = ""
    ebay_app_id: str = ""
    ebay_cert_id: str = ""
    apple_team_id: str = ""
    apple_bundle_id: str = ""
    apple_key_id: str = ""
    jwt_secret: str = ""
    database_url: str = ""
    environment: str = "development"
    pokescan_use_mock: bool = False


settings = Settings()
