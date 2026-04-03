# DMD Navigation Hub — API Specification

Two separate backends. Bearer token auth is preferred for programmatic use; hub session is
browser-obtained only (Cloudflare Turnstile blocks programmatic login).

---

## Authentication

### app.advhub.net — Bearer token

```
POST https://app.advhub.net/api/dmd_connector.php
Content-Type: application/json

{"email": "<email>", "password": "<password>"}
```

**⚠️ Each successful login issues a new `user_token` and invalidates the previous one.** Do not
re-authenticate unnecessarily.

Response fields (confirmed):

| Field | Type | Notes |
|---|---|---|
| `user_token` | string | 64-char hex. Use as `Authorization: Bearer <user_token>` |
| `token` | string | `rumo_` prefix. Device/group pairing token — NOT for API auth |
| `_id` | string | MongoDB user ID |
| `web_id` | string | WordPress user ID |
| `displayname` | string | |
| `email` | string | |
| `web_url` | string | User profile URL on drivemodedashboard.com |
| `avatar_url` | string | Full URL to avatar image |
| `share_location` | bool | Whether user has location sharing enabled |
| `location` | string | Encoded current location (format unknown) |
| `speed` | string | Current speed as string |
| `device` | string | Active device name e.g. `"DMD Navigation T865"` |
| `active_device_id` | string | UUID |
| `active_device_name` | string | |
| `active_device_at` | string | ISO 8601 timestamp of last device activity |
| `active_group_id` | string | Empty string when not in a group |
| `last_group_file_added` | string | Unix timestamp or `-1` |
| `active_group_file_time` | string | Unix timestamp or `-1` |
| `map_license` | string | `"true"` or `"false"` |
| `playstore` | string | `"true"` or `"false"` |
| `show_mph` | bool | Unit preference |
| `community_points` | int | |
| `groups_imported` | bool | |
| `profile_visibility` | string | e.g. `"Public"` |
| `signature` | string | Profile bio text |
| `instagram_url` | string | |
| `youtube_url` | string | |
| `facebook_url` | string | |
| `twitter_url` | string | |
| `email_verified` | bool | |
| `help` | bool | |
| `app_sig` | string | |
| `last_update` | string | Unix timestamp in ms |
| `bad_login_attempts` | int | |
| `_created` | int | Unix timestamp |
| `_modified` | int | Unix timestamp |
| `_state` | int | |

**Logout:**
```
POST https://app.advhub.net/api/logout.php
Authorization: Bearer <user_token>
```
Response: `{"message":"Logged out"}`

**Register:**
```
POST https://app.advhub.net/api/account_register.php
```
(Fields unknown. GET returns `{"message":"Method Not Allowed"}`)

---

### hub.dmdnavigation.com — Session cookie

No programmatic login. Cloudflare Turnstile CAPTCHA is enforced server-side on the login form,
regardless of user-agent or request format.

**Obtaining a session:** Log in via browser at `https://hub.dmdnavigation.com/account/login/`,
then copy the `dmdub_session` cookie value from DevTools → Application → Cookies.

```
Cookie: dmdub_session=<value>
```

Bearer tokens are rejected on hub endpoints (`{"error":"Authentication required"}`).

JSON API endpoints (`/api/gpx-manager.php`, `/api/gpx-collection/`) additionally require:
```
Origin: https://hub.dmdnavigation.com
Referer: https://hub.dmdnavigation.com/account/profile/gpx/
```

The upload endpoint and form-based location endpoints do NOT need these headers, but location
endpoints require a `csrf_token` from the page HTML.

---

## GPX Proxy — personal files (app.advhub.net)

All actions: `https://app.advhub.net/api/gpx_proxy.php`
Auth: `Authorization: Bearer <user_token>`

### List files

```
GET /api/gpx_proxy.php?action=list
```

Returns a JSON array of GPX file objects.

**Confirmed fields** (from live response, 140 files):

| Field | Type | Notes |
|---|---|---|
| `_id` | string | MongoDB ID |
| `owner` | string | User ID |
| `title` | string | |
| `file` | string | Stored filename e.g. `"69826d2ac2261.gpx"` |
| `show_on_map` | bool | "Loaded to DMD Map" state |
| `continent` | string | e.g. `"Europe"` |
| `country` | string | e.g. `"Germany"` |
| `difficulty` | string | e.g. `"Medium"` |
| `off_road_percentage` | int | 0–100 |
| `best_time` | array | Season strings |
| `vehicle` | array | Vehicle type strings |
| `tags` | string | Comma-separated |
| `description` | string | |
| `warnings` | string | |
| `image1_url` | string | |
| `image2_url` | string | |
| `youtube` | string | |
| `public` | bool | |
| `allow_download` | bool or null | |
| `allow_index` | bool or null | |
| `approved` | bool | |
| `rating` | null or number | |
| `rating_amount` | null or number | |
| `grid1` | string | Tile grid reference e.g. `"1000_15_13"` |
| `grid2` | string | |
| `grid3` | string | |
| `grid4` | string | |
| `_state` | int | |
| `_created` | int | Unix timestamp |
| `_modified` | int | Unix timestamp |
| `_mby` | null | |
| `_cby` | null | |

**Not present in list response** (contrary to earlier spec): `gpx_length_km`, `gpx_tracks_count`,
`gpx_waypoints_count`, `color`, `folder_id`, `folder_name`, `file_path`.

### Get single file

```
GET /api/gpx_proxy.php?action=get&id=<_id>
```

Returns a single GPX object. Same fields as list. Only works for files owned by the
authenticated user — returns `{"error":"Invalid or expired token"}` for other users' files.

### Toggle "Loaded to DMD Map"

```
POST /api/gpx_proxy.php?action=update_visibility
Authorization: Bearer <user_token>
Content-Type: application/json

{"data": {"_id": "<id>", "show_on_map": true}}
```

### Shared files list

```
GET /api/gpx_proxy.php?action=shares_list
```

Returns array of share objects. Fields:

| Field | Type | Notes |
|---|---|---|
| `_id` | string | Share record ID |
| `file_id` | string | ID of the shared GPX file |
| `user_id` | string | ID of the user who shared it |
| `user_email` | string | Email of the user who shared it |
| `show_on_map` | bool | |
| `_state` | int | |
| `_created` | int | Unix timestamp |
| `_modified` | int | Unix timestamp |

The shared file itself (identified by `file_id`) cannot be fetched via `action=get` with the
recipient's token — only the owner's token works.

---

## Locations Proxy (app.advhub.net)

All actions: `https://app.advhub.net/api/locations_proxy.php`
Auth: `Authorization: Bearer <user_token>`

### List

```
GET /api/locations_proxy.php?action=list
```

Returns a JSON array of location objects.

**Confirmed fields** (from live response, 48 locations):

| Field | Type | Notes |
|---|---|---|
| `_id` | string | MongoDB ID |
| `owner` | string | User ID |
| `title` | string | |
| `coordinates` | string | `"lat, lon"` e.g. `"49.310524, 8.559094"` |
| `continent` | string | |
| `country` | string | |
| `category` | array | e.g. `["Campground", "View Point"]` |
| `main_category` | string | |
| `ground` | string | |
| `crowded` | string | e.g. `"Rarely ever visited"` |
| `best_time` | array | Season strings |
| `vehicle` | array | Vehicle type strings |
| `tags` | array | String array (NOT comma-separated string) |
| `address` | string | |
| `description` | string | May contain HTML |
| `short_description` | string | |
| `warnings` | string | |
| `website` | string | |
| `phone` | string | |
| `youtube` | string | |
| `image1_url` | string | Relative path e.g. `"/storage/users/.../image.jpg"` |
| `image2_url` | string | |
| `show_on_map` | bool | "Visible On DMD2 Map" |
| `public` | bool | |
| `approved` | bool | |
| `grid1` | string | Tile grid reference |
| `grid2` | string | |
| `grid3` | string | |
| `grid4` | string | |
| `_state` | int | |
| `_created` | int | Unix timestamp |
| `_modified` | int | Unix timestamp |

**Note:** `tags` is returned as an array, not a comma-separated string.
`image1_url` is a relative path, not a full URL — prepend `https://hub.dmdnavigation.com`.

### Create

```
POST /api/locations_proxy.php?action=create
Authorization: Bearer <user_token>
Content-Type: application/json
```

The `action` must be a **query string parameter**, not in the JSON body.

Fields must be wrapped in a `"data"` key (same pattern as update):

```json
{"data": {"title": "...", "coordinates": "49.46, 8.63", "continent": "Europe",
          "country": "Germany", "category": ["Restaurant"], "main_category": "Restaurant"}}
```

Sending fields at the top level (without `"data"`) results in a 200 response with the record
created but all user fields silently discarded — confirmed from live testing.

Response: `{"_id": "<new_id>", "owner": "<user_id>", "_created": <ts>, "_modified": <ts>}`

### Update

```
POST /api/locations_proxy.php?action=update
Authorization: Bearer <user_token>
Content-Type: application/json

{"data": {"_id": "<id>", "<field>": "<value>"}}
```

### Delete

```
GET /api/locations_proxy.php?action=delete&id=<id>
```

The `id` must be a **query string parameter** — body fields `id` or `_id` do not work.
Response: `{"ok": true}`

---

## Group Proxy (app.advhub.net)

Endpoint: `https://app.advhub.net/api/group_proxy.php`
Auth: `Authorization: Bearer <user_token>`

The file exists and accepts requests. The only confirmed working action is `members`.
All other tested actions (get, list, create, join, leave, etc.) return `{"error":"Unknown action: ..."}`.

### Get group members

```
GET /api/group_proxy.php?action=members&group_id=<id>
```

The `group_id` must be a query string parameter — POST with JSON body does not work for this action.
Returns an array of member objects (empty array `[]` when group has no members).
The `group_id` format is not yet known — `active_group_id` in the login response is empty when
not in a group.

---

## GPX Manager — hub (hub.dmdnavigation.com)

All actions: `https://hub.dmdnavigation.com/api/gpx-manager.php`
Auth: `Cookie: dmdub_session=<value>` + `Origin: https://hub.dmdnavigation.com` + `Referer: https://hub.dmdnavigation.com/account/profile/gpx/`

### List files

```
GET /api/gpx-manager.php?action=list[&folder_id=<id>][&recursive=1]
```

### List all (flat, slim objects)

```
GET /api/gpx-manager.php?action=listAll
```

Returns slim objects with `id` (not `_id`), `title`, `file_path`, `file_size`, `folder_name`, `has_file`.

### List folders

```
GET /api/gpx-manager.php?action=list_folders
```

### Get GPX info

```
GET /api/gpx-manager.php?action=get_gpx_info&gpx_id=<id>
```

Returns full file object + parsed GPX content (tracks with points, length, etc.)

### Mutations (JSON POST)

```
POST /api/gpx-manager.php
Content-Type: application/json
Origin: https://hub.dmdnavigation.com
Referer: https://hub.dmdnavigation.com/account/profile/gpx/
```

| Action body | Description |
|---|---|
| `{"action":"toggle_show_on_map","gpx_id":"<id>","show_on_map":true}` | Toggle map visibility |
| `{"action":"create_folder","name":"<name>","parent_id":"<id_or_null>"}` | Create folder |
| `{"action":"move_item","item_type":"file","item_id":"<id>","target_folder_id":"<id>"}` | Move file |
| `{"action":"set_color","gpx_id":"<id>","color":"<color>"}` | Set color |
| `{"action":"duplicate","gpx_id":"<id>"}` | Duplicate |
| `{"action":"delete","gpx_id":"<id>"}` | Delete file |
| `{"action":"delete_folder","folder_id":"<id>"}` | Delete folder |

---

## GPX Upload (hub.dmdnavigation.com)

```
POST /account/profile/gpx/
Content-Type: multipart/form-data
Cookie: dmdub_session=<value>
```

No CSRF token, no Origin/Referer headers needed.

| Field | Required | Notes |
|---|---|---|
| `ajax_action` | **yes** | Must be exactly `"bulk_upload_gpx"` — omitting this returns the HTML page |
| `gpx_file` | yes | File content |
| `continent` | yes | e.g. `"Europe"` |
| `country` | yes | Country name |
| `gpx_meta_description` | no | From `<metadata><desc>` |
| `gpx_meta_author` | no | From `<metadata><author><name>` |
| `gpx_meta_link` | no | From `<metadata><link href>` |
| `gpx_meta_keywords` | no | From `<metadata><keywords>` |
| `folder_id` | no | Target folder ID |

Response: `{"success":true,"gpx_id":"<id>","title":"...","length_km":1.34,"tracks":1,"routes":0,"waypoints":0}`

---

## GPX Download (hub.dmdnavigation.com)

```
GET /api/gpx-download/?id=<gpx_id>
Cookie: dmdub_session=<value>
```
Authenticated download. Returns `application/xml`.

```
GET /api/gpx-view/?id=<gpx_id>
```
Public download — no auth required.

---

## Community GPX Collection (hub.dmdnavigation.com)

All actions: `https://hub.dmdnavigation.com/api/gpx-collection/`
Auth: `Cookie: dmdub_session=<value>` + Origin/Referer headers (same as gpx-manager).
Bearer token is rejected.

### List
```
GET /api/gpx-collection/?action=list[&folder_id=<id>][&recursive=1][&vehicles=...][&difficulties=...][&tags=...][&loaded=...][&color=...]
```

### Mutations (JSON POST)

| Action | Notes |
|---|---|
| `{"action":"toggle_show_on_map","file_id":"<id>","show_on_map":true}` | Uses `file_id`, not `gpx_id` |
| `{"action":"remove_from_collection","file_id":"<id>"}` | |
| `{"action":"move_community_item","file_id":"<id>","target_folder_id":"<id>"}` | |

---

## Locations — hub (hub.dmdnavigation.com)

Form-based. All POSTs require `csrf_token` from page HTML.
Obtain: `GET /account/profile/locations/` → extract `<input name="csrf_token" value="...">`.
Location data is embedded in the page as a JS array — no JSON list endpoint.

### Add
```
POST /account/profile/locations/add
Content-Type: application/x-www-form-urlencoded
```

### Edit
```
POST /account/profile/locations/edit/<location_id>
Content-Type: application/x-www-form-urlencoded
```

### Toggle show_on_map
```
POST /account/profile/locations/
csrf_token=<t>&_id=<id>&submit_toggle_show_on_map=1&current_show_on_map=<0|1>&show_on_map=<1|0>
```

### Delete
```
POST /account/profile/locations/
csrf_token=<t>&_id=<id>&submit_change_location_delete=1
```

### Import (parse step)
```
POST /account/profile/locations/
Content-Type: multipart/form-data
import_file=<file>&submit_parse_locations=1
```
Response: `{"success":true,"locations":[...]}`

### Import (confirm step)
```
POST /account/profile/locations/
Content-Type: application/json
{"submit_import_selected":"1","locations":[...],"import_show_on_map":true}
```
Response: `{"success":true,"imported":3,"skipped":0}`

---

## Reverse Geocode Proxy (hub.dmdnavigation.com)

```
POST /core/helpers/locations/nominatim_proxy.php
Content-Type: application/json

{"action":"reverse","lat":49.3105,"lon":8.5586,"zoom":5}
```

Returns Nominatim-style response. Used to auto-populate continent/country fields.

---

## Live Location (app.advhub.net)

Auth: `Authorization: Bearer <user_token>`

### Broadcast location

```
POST /api/location_update.php
Authorization: Bearer <user_token>
Content-Type: application/json

{"data": {"_id": "<user_id>", "lat": <float>, "lon": <float>, "speed": <float>, "heading": <int>, "altitude": <float>, "accuracy": <float>}}
```

`_id` must match the token owner — other values return `{"error":"Forbidden — _id does not match token owner"}`.

Response: `{"ok": true}`

**Rate limited.** Too-frequent POSTs return `{"error":"Rate limit exceeded","retry_after_ms":<ms>}`. DMD posts at roughly 1 Hz.

Additional optional fields (confirmed present in DMD code): `bearing`, `online`, `active`, `device`, `user_id`.

The login response reflects the current state: `share_location` (bool), `speed` (string), `location` (custom-encoded string — format unknown), `active_device_id`, `active_device_at`.

### Poll for incoming navigate-to

```
GET /api/navigate-to.php
Authorization: Bearer <user_token>
```

Response: `{"success":true,"data":{"has_destination":false}}`

Used by DMD to detect when a group member has pushed a navigate-to target. Poll on a timer.

### Clear navigate-to destination

```
POST /api/navigate-to.php
Authorization: Bearer <user_token>
Content-Type: application/json

{"action": "clear"}
```

Response: `{"success":true}`

The action name for *sending* a destination to group members is not yet confirmed (requires an active group context to test).

### Group members with live location

```
GET /api/group_proxy.php?action=members&group_id=<id>
Authorization: Bearer <user_token>
```

Returns an array of `UserInfo` objects. Confirmed fields (from DMD source):

| Field | Notes |
|---|---|
| `_id` | MongoDB user ID |
| `name` / `displayName` / `displayname` | Display name variants |
| `avatar_url` | Avatar image URL |
| `color` | Rider color on map |
| `latitude` / `longitude` | Current GPS position |
| `altitude` | Metres |
| `accuracy` | GPS accuracy |
| `speed` | Current speed |
| `heading` / `bearing` | Direction |
| `online` | Boolean |
| `active` | Boolean |
| `share_location` | Boolean |
| `last_update` | Timestamp |
| `token` / `user_token` | Group pairing token / auth token |
| `device` / `device_name` | Active device |
| `location` | Encoded location string (same format as login response) |

`group_id` comes from `active_group_id` in the login response (empty string when not in a group).

---

## Apps Proxy (app.advhub.net)

```
GET /api/apps_proxy.php
Authorization: Bearer <user_token>
```

Returns current version info and APK download URLs for all DMD ecosystem apps:
`dmd2_version`, `dmd2_apk_url`, `buttons_version`, `buttons_apk_url`, `auto_off_version`, `auto_off_apk_url`, `sos_version`, `sos_apk_url`, `lora_version`, `lora_apk_url`, and corresponding `*_changes` strings.

---

## Hub Locations Proxy (app.advhub.net)

```
GET /api/hub_locations_proxy.php
Authorization: Bearer <user_token>
```

Returns community/public locations from all users. Same field structure as `locations_proxy.php`.

---

## Other Confirmed Hosts

| Host | Purpose |
|---|---|
| `app.advhub.net` | Main app API (Bearer token) |
| `api.advhub.net` | Group GPX file storage |
| `hub.dmdnavigation.com` | Web hub (session cookie) |
| `router.advhub.net` | BRouter routing engine (`/api/brouter`) |
| `fastmaps.advhub.net` | Map tile server |
| `dmd-maps.b-cdn.net` | Map tiles CDN, speed camera GPX files, routing data |

### Group GPX (api.advhub.net)

```
POST https://api.advhub.net/group_gpx/upload.php
Content-Type: multipart/form-data
```

```
POST https://api.advhub.net/group_gpx/delete.php
```

Storage base URL: `https://api.advhub.net/group_gpx/uploads/`

### User profile / avatar

```
GET https://app.advhub.net/users/view/<user_id>
```

Avatar storage: `https://app.advhub.net/storage/users/<user_id>/`

---

## iOS API Namespace (app.advhub.net) — preferred mobile path

Discovered by intercepting the official iOS companion app (TestFlight) via Charles Proxy with SSL
proxying enabled for `*.advhub.net`.

**All endpoints:** `https://app.advhub.net/api/ios/`

**Auth:** `Authorization: Bearer <ios_bearer_token>`

The iOS Bearer token is different from the raw `user_token`. It is constructed as:
```
base64(<_id> + ":" + <epoch_seconds_at_login> + ":" + <user_token>)
```
`epoch_seconds_at_login` is the Unix timestamp (seconds) at the moment the `/api/dmd_connector.php`
login was performed and `user_token` was issued. The token **must** be re-constructed using the
original login timestamp — it is not refreshed when the session is refreshed.

### iOS-specific Bearer token — planner session bridge

```
GET /api/ios/planner-session
Authorization: Bearer <ios_bearer_token>
```

Returns a `dmdub_session` cookie value that can be used as `Cookie: dmdub_session=<value>` on
`hub.dmdnavigation.com` endpoints — bypassing the Cloudflare Turnstile CAPTCHA.

Response: `{"session": "<dmdub_session_value>"}`

---

### GPX — iOS namespace

#### List files (hierarchical)

```
GET /api/ios/my-gpx?action=list[&folder_id=<id>]
Authorization: Bearer <ios_bearer_token>
```

Response envelope:
```json
{"files": [...], "folders": [...]}
```

To get all files: start with no `folder_id`, then recurse into each folder that has
`file_count > 0` or `folder_count > 0`.

**File object fields** (confirmed from live response):

| Field | Type | Notes |
|---|---|---|
| `id` | string | ⚠️ `id`, not `_id` |
| `title` | string | |
| `country` | string | |
| `continent` | string | |
| `show_on_map` | bool | |
| `is_public` | bool | ⚠️ `is_public`, not `public` |
| `description` | string | |
| `distance_km` | float | Route distance |
| `tracks_count` | int | |
| `waypoints_count` | int | |
| `color` | string | Hex color or empty |
| `allow_download` | bool | |

**Folder object fields:**

| Field | Type | Notes |
|---|---|---|
| `id` | string | |
| `name` | string | |
| `file_count` | int | |
| `folder_count` | int | |

#### Toggle map visibility

```
POST /api/ios/my-gpx
Authorization: Bearer <ios_bearer_token>
Content-Type: application/json

{"action": "toggle_show_on_map", "gpx_id": "<id>", "show_on_map": true}
```

#### Download file content

```
GET /api/ios/gpx/<id>/content
Authorization: Bearer <ios_bearer_token>
```

Returns raw GPX XML bytes (`application/gpx+xml`).

#### Upload

⚠️ **Not yet confirmed.** The iOS app's upload flow triggers an error when SSL proxying is
active — possible certificate pinning or Cloudflare challenge on that specific endpoint.
Discovery is pending.

---

### Locations — iOS namespace

All location mutations use a single endpoint with an `action` field in the body. No CSRF required.

```
POST /api/ios/my-locations
Authorization: Bearer <ios_bearer_token>
Content-Type: application/json
```

#### List

```
GET /api/ios/my-locations
Authorization: Bearer <ios_bearer_token>
```

Response envelope: `{"success": true, "locations": [...]}`

**Location object fields** (confirmed from live response):

| Field | Type | Notes |
|---|---|---|
| `id` | string | ⚠️ `id`, not `_id` |
| `title` | string | |
| `latitude` | float | ⚠️ Separate float, not `"lat, lon"` string |
| `longitude` | float | |
| `continent` | string | |
| `country` | string | |
| `category` | array | String array |
| `main_category` | string | |
| `show_on_map` | bool | |
| `is_public` | bool | ⚠️ `is_public`, not `public` |
| `short_description` | string | |
| `address` | string | |
| `thumbnail_url` | string | |

#### Create

```json
{"action": "create", "title": "...", "latitude": 49.46, "longitude": 8.63,
 "continent": "Europe", "country": "Germany",
 "category": ["Campground"], "main_category": "Campground",
 "public": false, "show_on_map": false}
```

#### Update

```json
{"action": "update", "location_id": "<id>", "title": "...",
 "latitude": 49.46, "longitude": 8.63,
 "continent": "Europe", "country": "Germany",
 "category": ["Campground"], "main_category": "Campground"}
```

#### Toggle map visibility

```json
{"action": "update", "location_id": "<id>", "show_on_map": true}
```

#### Delete

```json
{"action": "delete", "location_id": "<id>"}
```

---

### Navigate-to — iOS namespace

```
POST /api/ios/navigate-to
Authorization: Bearer <ios_bearer_token>
Content-Type: application/json

{"action": "set", "lat": <float>, "lng": <float>, "name": "<label>"}
```

Note: field is `lng`, not `lon`.
