# DMD Navigation Hub — API Specification

Base URL: `https://hub.dmdnavigation.com`

## Authentication

All endpoints require a session cookie. There is no programmatic login — the login form is protected by a Cloudflare Turnstile CAPTCHA that is enforced server-side regardless of user-agent or request format. No JSON auth endpoint exists.

**Obtaining a session:** Log in via the browser at `https://hub.dmdnavigation.com/account/login/`, then copy the `dmdub_session` cookie value from DevTools (Application → Cookies).

```
Cookie: dmdub_session=<value>
```

Sessions persist across requests as long as the cookie is valid.

**JSON API endpoints** (`/api/gpx-manager.php`, `/api/gpx-collection/`) require `Origin: https://hub.dmdnavigation.com` and `Referer: https://hub.dmdnavigation.com/account/profile/gpx/` headers in addition to the session cookie. No CSRF token needed.

**Upload endpoint** (`/account/profile/gpx/` with `ajax_action=bulk_upload_gpx`) requires only the session cookie — no CSRF token, no Origin/Referer header.

The form-based location endpoints (`/account/profile/locations/`) do require a `csrf_token` field from the page HTML.

---

## GPX Manager — own files

All actions go through `/api/gpx-manager.php`.

### List files

```
GET /api/gpx-manager.php?action=list
```

Optional query parameters:

| Parameter | Description |
|---|---|
| `folder_id` | List contents of a specific folder |
| `recursive=1` | Include files in subfolders |

Response:

```json
{
  "success": true,
  "files": [ /* GPX file objects */ ],
  "folders": [ /* Folder objects */ ],
  "current_folder": null,
  "current_folder_parent_id": null,
  "search_mode": false,
  "folders_with_matches": [],
  "has_filters": false
}
```

### List all files (flat)

```
GET /api/gpx-manager.php?action=listAll
```

Response:

```json
{
  "success": true,
  "files": [ /* GPX file objects */ ],
  "total_count": 0
}
```

### List folders

```
GET /api/gpx-manager.php?action=list_folders
```

Response:

```json
{
  "success": true,
  "folders": []
}
```

### Get GPX info

```
GET /api/gpx-manager.php?action=get_gpx_info&gpx_id=<id>
```

Response:

```json
{
  "success": true,
  "gpx": { /* GPX file object */ },
  "parsed": { /* Parsed GPX content metadata */ }
}
```

### Toggle "Loaded to DMD Map"

```
POST /api/gpx-manager.php
Content-Type: application/json

{ "action": "toggle_show_on_map", "gpx_id": "<id>", "show_on_map": true }
```

### Create folder

```
POST /api/gpx-manager.php
Content-Type: application/json

{ "action": "create_folder", "name": "<name>", "parent_id": "<parent_folder_id_or_null>" }
```

### Move item

```
POST /api/gpx-manager.php
Content-Type: application/json

{ "action": "move_item", "item_type": "file|folder", "item_id": "<id>", "target_folder_id": "<id>" }
```

### Set color

```
POST /api/gpx-manager.php
Content-Type: application/json

{ "action": "set_color", "gpx_id": "<id>", "color": "<color>" }
```

### Duplicate

```
POST /api/gpx-manager.php
Content-Type: application/json

{ "action": "duplicate", "gpx_id": "<id>" }
```

### Delete file

```
POST /api/gpx-manager.php
Content-Type: application/json

{ "action": "delete", "gpx_id": "<id>" }
```

### Delete folder

```
POST /api/gpx-manager.php
Content-Type: application/json

{ "action": "delete_folder", "folder_id": "<id>" }
```

---

## Upload GPX

Requires `dmdub_session` cookie. No CSRF token, no Origin/Referer needed.

```
POST /account/profile/gpx/
Content-Type: multipart/form-data
```

| Field | Required | Description |
|---|---|---|
| `ajax_action` | yes | Must be `"bulk_upload_gpx"` |
| `gpx_file` | yes | GPX file content |
| `continent` | yes | e.g. `"Europe"`, `"Asia"`, `"North America"` |
| `country` | yes | Country name |
| `gpx_meta_description` | no | From GPX `<metadata><desc>` |
| `gpx_meta_author` | no | From GPX `<metadata><author><name>` |
| `gpx_meta_link` | no | From GPX `<metadata><link href>` |
| `gpx_meta_keywords` | no | From GPX `<metadata><keywords>` |
| `folder_id` | no | Target folder ID |

Response:

```json
{
  "success": true,
  "gpx_id": "<id>",
  "title": "<derived title>",
  "length_km": 42.5,
  "tracks": 1,
  "routes": 0,
  "waypoints": 0
}
```

On failure: `{ "success": false, "error": "..." }`

**Note:** There is no GPX upload path on `app.advhub.net`. Upload requires the hub session cookie.

The hub automatically reverse-geocodes the first track point to populate continent/country if not provided. The client-side JS uses `/core/helpers/locations/nominatim_proxy.php` for this:

```
POST /core/helpers/locations/nominatim_proxy.php
Content-Type: application/json

{ "action": "reverse", "lat": 49.3105, "lon": 8.5586, "zoom": 5 }
```

---

## Community Collection

Endpoints for GPX files shared/saved from the community hub (not uploaded by the authenticated user).

### List collection files

```
GET /api/gpx-collection/?action=list
```

Optional query parameters:

| Parameter | Description |
|---|---|
| `folder_id` | Scope to folder |
| `recursive=1` | Include subfolders |
| `vehicles` | Comma-separated vehicle types |
| `difficulties` | Comma-separated difficulty levels |
| `tags` | Comma-separated tags |
| `loaded` | Filter by loaded status |
| `color` | Filter by color |

Response:

```json
{
  "success": true,
  "files": [ /* GPX file objects */ ]
}
```

### Toggle "Loaded to DMD Map" (collection file)

```
POST /api/gpx-collection/
Content-Type: application/json

{ "action": "toggle_show_on_map", "file_id": "<id>", "show_on_map": true }
```

Note: uses `file_id` (not `gpx_id`) for collection files.

### Remove from collection

```
POST /api/gpx-collection/
Content-Type: application/json

{ "action": "remove_from_collection", "file_id": "<id>" }
```

### Move community item

```
POST /api/gpx-collection/
Content-Type: application/json

{ "action": "move_community_item", "file_id": "<id>", "target_folder_id": "<id>" }
```

---

## Download / Export

### Download single file

```
GET /api/gpx-download/?id=<gpx_id>
```

Returns the GPX file as `application/xml`.

### Download public file (no auth required)

```
GET /api/gpx-view/?id=<gpx_id>
```

Returns the GPX file as `application/xml`.

### Export multiple files

```
POST /api/gpx-export.php
Content-Type: application/json

{ "file_ids": ["<id1>", "<id2>"] }
```

---

## GPX File Object

### Full object (`action=list`, `action=get_gpx_info`)

| Field | Type | Description |
|---|---|---|
| `_id` | string | Unique ID (MongoDB-style hex) |
| `owner` | string | Owner user ID |
| `title` | string | Display title |
| `description` | string | |
| `warnings` | string | |
| `continent` | string | e.g. `"Europe"` |
| `country` | string | e.g. `"Germany"` |
| `best_time` | array | e.g. `["Spring", "Summer"]` |
| `vehicle` | array | Vehicle types |
| `difficulty` | string | e.g. `"Medium"` |
| `off_road_percentage` | number | 0–100 |
| `file` | string | Stored filename |
| `file_path` | string | Storage path (relative URL) |
| `image1_url` | string | |
| `image2_url` | string | |
| `youtube` | string | YouTube URL |
| `public` | boolean | Publicly visible |
| `approved` | boolean | |
| `show_on_map` | boolean | "Loaded to DMD Map" state |
| `allow_index` | boolean | |
| `allow_download` | boolean | |
| `tags` | string | Comma-separated tags |
| `gpx_length_km` | number | Total track length |
| `gpx_tracks_count` | number | |
| `gpx_routes_count` | number | |
| `gpx_waypoints_count` | number | |
| `gpx_meta_time` | string | Timestamp from GPX `<time>` metadata |
| `color` | string | User-assigned colour |
| `_created` | number | Unix timestamp |
| `_modified` | number | Unix timestamp |
| `_state` | number | |
| `_model` | string | Always `"gpx"` |

### Slim object (`action=listAll`)

| Field | Type | Description |
|---|---|---|
| `id` | string | Unique ID (note: `id` not `_id`) |
| `title` | string | |
| `file_path` | string | Storage path |
| `file_size` | number | Bytes |
| `folder_name` | string | e.g. `"Root"` |
| `has_file` | boolean | |

### Parsed GPX content (`action=get_gpx_info` → `parsed`)

```json
{
  "valid": true,
  "name": "",
  "description": "...",
  "author": "",
  "tracks": [
    {
      "name": "Track name",
      "description": "",
      "points_count": 4642,
      "length_km": 286.21,
      "points": [
        { "lat": 48.528, "lon": 8.837, "ele": null, "time": null }
      ]
    }
  ]
}
```

---

## Locations

All location management is handled via form POSTs to the page URLs (not a separate JSON API). The page is at `/account/profile/locations/`.

All POST requests require a `csrf_token` field. Obtain it from the page HTML (`<input type="hidden" name="csrf_token" value="...">`).

### List locations

```
GET /account/profile/locations/
```

Location data is embedded in the page as a JavaScript array — there is no dedicated JSON list endpoint. Filter parameters can be passed as query strings to the GET request.

### Add location

```
POST /account/profile/locations/add
Content-Type: application/x-www-form-urlencoded
```

| Field | Type | Description |
|---|---|---|
| `csrf_token` | string | Required |
| `title` | string | Location name |
| `coordinates` | string | `"lat, lon"` e.g. `"49.310519, 8.558673"` |
| `continent_specific` | string | e.g. `"Europe"` |
| `country_specific` | string | Country name |
| `category[]` | string (repeat) | One or more from the category list below |
| `main_category` | string | Primary category |
| `ground` | string | Ground type |
| `crowded` | string | |
| `best_time[]` | string (repeat) | `Spring`, `Summer`, `Autumn`, `Winter` |
| `vehicle[]` | string (repeat) | Vehicle types (see list below) |
| `public` | `true`/`false` | Publicly visible (requires admin approval) |
| `show_on_map` | `true`/`false` | "Visible On DMD2 Map" |
| `short_description` | string | |
| `description` | string | |
| `warnings` | string | |
| `address` | string | |
| `tags` | string | Comma-separated |
| `website` | string | |
| `phone` | string | |
| `image_primary` | file | Primary image upload |
| `image_secondary` | file | Secondary image upload |
| `youtube_link` | string | YouTube URL |

**Category values:**
Fuel Station, Restaurant, Accommodation, View Point, Cave, Campground, Peak, Wild Camping Spot, Water Source, Picnic Area, River or Lake Side, Historic / Ruins, Monument, Natural Highlight, Motorcycle & Car Parking, RV Parking, Swim (Lake / River / Beach), Start of a Good Trail, Other (will not be public), Fence (will not be public), Home (will not be public), Work (will not be public), Danger (will not be public)

**Vehicle values:**
Road Only Vehicle, Adventure Motorcycle, Enduro Motorcycle, 4x4 Car / Pickup / Small Van, 4x4 Big Vans / RV, ATV, SSV

### Edit location

```
POST /account/profile/locations/edit/<location_id>
Content-Type: application/x-www-form-urlencoded
```

Same fields as Add. The location ID is in the URL path.

### Toggle "Visible On DMD2 Map"

```
POST /account/profile/locations/
Content-Type: application/x-www-form-urlencoded
```

| Field | Value |
|---|---|
| `csrf_token` | current token |
| `_id` | location ID |
| `submit_toggle_show_on_map` | `1` |
| `current_show_on_map` | `0` or `1` (current state) |
| `show_on_map` | `1` or `0` (desired new state) |

### Delete location

```
POST /account/profile/locations/
Content-Type: application/x-www-form-urlencoded
```

| Field | Value |
|---|---|
| `csrf_token` | current token |
| `_id` | location ID |
| `submit_change_location_delete` | `1` |

### Import locations (parse step)

```
POST /account/profile/locations/
Content-Type: multipart/form-data
```

| Field | Description |
|---|---|
| `import_file` | File to parse (GPX, KML, etc.) |
| `submit_parse_locations` | `1` |

Response:

```json
{
  "success": true,
  "locations": [ /* parsed location objects */ ]
}
```

### Import locations (confirm step)

```
POST /account/profile/locations/
Content-Type: application/json

{
  "submit_import_selected": "1",
  "locations": [ /* location objects from parse step */ ],
  "import_show_on_map": true
}
```

Response:

```json
{
  "success": true,
  "imported": 3,
  "skipped": 0
}
```

### Location object

Fields embedded in the page's JS data (map overlay):

| Field | Type | Description |
|---|---|---|
| `_id` | string | Unique ID |
| `title` | string | |
| `coordinates` | string | `"lat, lon"` |
| `category` | array | e.g. `["Campground", "View Point"]` |
| `main_category` | string | |
| `ground` | string | |
| `vehicle` | array | |
| `image1_url` | string | |
| `show_on_map` | boolean | |
| `is_favorite` | boolean | |

---

## Public Browse Endpoints

These do not require authentication.

### Browse community GPX list (fragment)

```
GET /gpx_frag/gpx_list_fragment/
```

Returns an HTML fragment of the community GPX listing.

### GPX map data

```
GET /api/gpx-map-data
```

Returns map overlay data for the GPX browse page.

### View a community GPX entry

```
GET /gpx/view/<gpx_id>/
```

Returns the HTML page for a community-shared GPX file.

---

## Android App API (app.advhub.net)

The DMD2 Android app (`com.thorkracing.dmd2launcher`) and other clients (Rumo web planner) communicate with a backend at `app.advhub.net`. This is the preferred programmatic API — unlike `hub.dmdnavigation.com`, it has no CAPTCHA on login.

### Hosts

| Host | Purpose |
|---|---|
| `app.advhub.net` | Main app API, GPX proxy, locations proxy |
| `api.advhub.net` | File storage and group GPX uploads |
| `router.advhub.net` | BRouter-based route calculation |
| `fastmaps.advhub.net` | Map tile CDN |

### Authentication

Uses `Bearer` token authentication. The token is the `user_token` field from the login response.

```
Authorization: Bearer <user_token>
```

**Login:**

```
POST https://app.advhub.net/api/dmd_connector.php
Content-Type: application/json

{ "email": "<email>", "password": "<password>" }
```

Response includes (among other profile fields):

| Field | Description |
|---|---|
| `_id` | MongoDB user ID |
| `user_token` | Bearer token for API auth (64-char hex) |
| `token` | Device/group pairing token (`rumo_` prefix) — not for API auth |
| `web_id` | WordPress user ID |

The `user_token` expires and must be refreshed by re-authenticating. A 401 response indicates an expired token.

The same endpoint is also accessible at `https://hub.dmdnavigation.com/api/dmd_connector.php` (same response, no session cookie set).

**Logout:** `POST https://app.advhub.net/api/logout.php`

**Register:** `POST https://app.advhub.net/api/account_register.php`

### GPX Proxy (personal files)

All actions go through `https://app.advhub.net/api/gpx_proxy.php`.

#### List files

```
GET https://app.advhub.net/api/gpx_proxy.php?action=list
```

Returns a JSON array of GPX file objects. Each object includes `_id`, `title`, `show_on_map`, `file_path`, `gpx_length_km`, `continent`, `country`, `color`, `folder_id`, `folder_name`, `_created`, `_modified`.

#### Get single file

```
GET https://app.advhub.net/api/gpx_proxy.php?action=get&id=<id>
```

#### Toggle "Loaded to DMD Map"

```
POST https://app.advhub.net/api/gpx_proxy.php?action=update_visibility
Content-Type: application/json

{ "data": { "_id": "<id>", "show_on_map": true } }
```

#### Shared files list

```
GET https://app.advhub.net/api/gpx_proxy.php?action=shares_list
```

#### Toggle shared file visibility

```
POST https://app.advhub.net/api/gpx_proxy.php?action=update_share_visibility
```

### Locations Proxy (personal locations)

All actions go through `https://app.advhub.net/api/locations_proxy.php`.

#### List locations

```
GET https://app.advhub.net/api/locations_proxy.php?action=list
```

Returns a JSON array. Each object has the same fields as the hub location object (see Location object section), plus `owner` and other metadata.

#### Create location

```
POST https://app.advhub.net/api/locations_proxy.php?action=create
Content-Type: application/json
```

Body: location fields (see hub Add location for field list). Required: `title`, `coordinates` (`"lat, lon"`), `continent`, `country`, `category` (array), `main_category`. Optional: `show_on_map`, `public`, `description`, `short_description`, `warnings`, `tags`, `vehicle` (array), `best_time` (array), `ground`, `address`, `website`, `phone`, `youtube`.

Response: `{ "_id": "<new_id>", "owner": "<user_id>", ... }`

#### Update location

```
POST https://app.advhub.net/api/locations_proxy.php?action=update
Content-Type: application/json

{ "data": { "_id": "<id>", "<field>": "<value>", ... } }
```

#### Delete location

```
GET/POST https://app.advhub.net/api/locations_proxy.php?action=delete&id=<id>
```

The `id` must be a query string parameter.

### Group GPX (group ride file sync)

```
POST https://api.advhub.net/group_gpx/upload.php
Content-Type: multipart/form-data
```

```
POST https://api.advhub.net/group_gpx/delete.php
```

Storage base: `https://api.advhub.net/group_gpx/uploads/`

### User profile

```
GET https://app.advhub.net/users/view/<user_id>
```

User avatar storage: `https://app.advhub.net/storage/users/<user_id>/`

Legacy avatar path also found at: `https://hub.dmdnavigation.com/storage/users/`
