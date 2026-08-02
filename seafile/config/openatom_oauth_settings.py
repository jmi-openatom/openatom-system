import os


def _required(name):
    value = os.environ.get(name, "").strip()
    if not value:
        raise RuntimeError(f"{name} must not be empty")
    return value


def _boolean(name, default=False):
    value = os.environ.get(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


ENABLE_OAUTH = True
OAUTH_CREATE_UNKNOWN_USER = True
OAUTH_ACTIVATE_USER_AFTER_CREATION = True
OAUTH_ENABLE_INSECURE_TRANSPORT = False

OAUTH_CLIENT_ID = _required("SEAFILE_OAUTH_CLIENT_ID")
OAUTH_CLIENT_SECRET = _required("SEAFILE_OAUTH_CLIENT_SECRET")
OAUTH_REDIRECT_URL = _required("SEAFILE_OAUTH_REDIRECT_URL")

OAUTH_PROVIDER = "jmi-openatom"
OAUTH_PROVIDER_DOMAIN = "jmi-openatom"
OAUTH_AUTHORIZATION_URL = _required("SEAFILE_OAUTH_AUTHORIZE_URL")
OAUTH_TOKEN_URL = _required("SEAFILE_OAUTH_TOKEN_URL")
OAUTH_USER_INFO_URL = _required("SEAFILE_OAUTH_USERINFO_URL")
OAUTH_SCOPE = ["openid", "profile", "email"]
OAUTH_ATTRIBUTE_MAP = {
    "sub": (True, "uid"),
    "name": (False, "name"),
    "email": (False, "contact_email"),
}

# Keep the local admin login available by default for disaster recovery.
DISABLE_ADFS_USER_PWD_LOGIN = _boolean("SEAFILE_DISABLE_PASSWORD_LOGIN", False)
CLIENT_SSO_VIA_LOCAL_BROWSER = True
