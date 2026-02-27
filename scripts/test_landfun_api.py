import requests
import random

import string
import sys

BASE_URL = "http://localhost:8080"
ADMIN_EMAIL = "admin@landfun.com"
ADMIN_PASSWORD = "password"


class LandfunClient:
    def __init__(self, email, password):
        self.email = email
        self.password = password
        self.token = None
        self.user_id = None
        self.login()

    def login(self):
        url = f"{BASE_URL}/auth/login"
        payload = {"email": self.email, "password": self.password}
        response = requests.post(url, json=payload)
        if response.status_code == 200:
            data = response.json()
            if data["code"] == 200:
                self.token = data["data"]["token"]
                self.user_id = data["data"]["user"]["id"]
                # print(f"Logged in as {self.email}, Token: {self.token[:10]}...")
            else:
                raise Exception(f"Login failed for {self.email}: {data['message']}")
        else:
            raise Exception(
                f"Login request failed for {self.email}: {response.status_code} {response.text}"
            )

    def get_headers(self):
        return {"Authorization": f"Bearer {self.token}"}

    def post(self, path, payload):
        return requests.post(
            f"{BASE_URL}{path}", json=payload, headers=self.get_headers()
        )

    def get(self, path, params=None):
        return requests.get(
            f"{BASE_URL}{path}", params=params, headers=self.get_headers()
        )

    def put(self, path, payload):
        return requests.put(
            f"{BASE_URL}{path}", json=payload, headers=self.get_headers()
        )

    def delete(self, path):
        return requests.delete(f"{BASE_URL}{path}", headers=self.get_headers())


def generate_random_string(length=8):
    return "".join(random.choices(string.ascii_lowercase + string.digits, k=length))


def main():
    print("=== Starting Landfun API Test ===")

    try:
        admin = LandfunClient(ADMIN_EMAIL, ADMIN_PASSWORD)
        print("1. Admin logged in successfully.")
    except Exception as e:
        print(f"FAILED to login as Admin: {e}")
        print("Make sure the backend is running at http://localhost:8080")
        sys.exit(1)

    # 2. Setup Data
    print("2. Setting up test data...")

    # Get Menu Tree to find permissions
    menu_resp = admin.get("/sys/menu/tree")
    resp_json = menu_resp.json()
    print(f"  Menu tree response: {resp_json}")
    menus = resp_json.get("data")
    if menus is None:
        print("  WARNING: Menu data is None. Initializing as empty list.")
        menus = []

    def find_menu_id_by_permission(menu_list, permission):
        if not menu_list:
            return None
        for m in menu_list:
            if m.get("permission") == permission:
                return m["id"]
            if "children" in m and m["children"]:
                res = find_menu_id_by_permission(m["children"], permission)
                if res:
                    return res
        return None

    # For simplicity, we can also just fetch all menu ids for super admin

    def collect_all_menu_ids(menu_list):
        ids = []
        for m in menu_list:
            ids.append(m["id"])
            if "children" in m and m["children"]:
                ids.extend(collect_all_menu_ids(m["children"]))
        return list(set(ids))

    all_menu_ids = collect_all_menu_ids(menus)
    user_list_perm_id = find_menu_id_by_permission(menus, "sys:user:list")
    dept_list_perm_id = find_menu_id_by_permission(menus, "sys:dept:list")

    # 2.1 Create Departments
    print("  Creating Departments...")
    depts = {}
    # Dept A
    resp = admin.post("/sys/dept/create", {"name": "Dept A", "parentId": 0})

    depts["A"] = resp.json()["data"]["id"]
    # Dept B (under A)
    resp = admin.post("/sys/dept/create", {"name": "Dept B", "parentId": depts["A"]})
    depts["B"] = resp.json()["data"]["id"]
    # Dept C (under B)
    resp = admin.post("/sys/dept/create", {"name": "Dept C", "parentId": depts["B"]})
    depts["C"] = resp.json()["data"]["id"]
    # Dept D
    resp = admin.post("/sys/dept/create", {"name": "Dept D", "parentId": 0})

    depts["D"] = resp.json()["data"]["id"]
    # Dept E
    resp = admin.post("/sys/dept/create", {"name": "Dept E", "parentId": 0})

    depts["E"] = resp.json()["data"]["id"]

    print(f"  Created depts: {depts}")

    # 2.2 Create Roles
    print("  Creating Roles...")
    roles = {}

    # SuperManager (ALL scope, all menus)
    resp = admin.post(
        "/sys/role",
        {
            "name": "SuperManager",
            "code": "super_mgr_" + generate_random_string(4),
            "description": "Full access",
            "dataScope": "ALL",
            "menuIds": all_menu_ids,
            "deptIds": [],
        },
    )
    roles["SuperManager"] = resp.json()["data"]["id"]

    # AreaManager (RECURSIVE, User List, Dept List)
    resp = admin.post(
        "/sys/role",
        {
            "name": "AreaManager",
            "code": "area_mgr_" + generate_random_string(4),
            "description": "Recursive dept access",
            "dataScope": "DEPT_RECURSIVE",
            "menuIds": [user_list_perm_id, dept_list_perm_id],
            "deptIds": [],
        },
    )
    roles["AreaManager"] = resp.json()["data"]["id"]

    # CustomViewer (CUSTOM, Dept List, assigned to A & B)
    resp = admin.post(
        "/sys/role",
        {
            "name": "CustomViewer",
            "code": "custom_view_" + generate_random_string(4),
            "description": "Custom dept access",
            "dataScope": "DEPT_CUSTOM",
            "menuIds": [dept_list_perm_id],
            "deptIds": [depts["A"], depts["B"]],
        },
    )
    roles["CustomViewer"] = resp.json()["data"]["id"]

    print(f"  Created roles: {roles}")

    # 2.3 Create Users
    print("  Creating Users...")
    user_configs = [
        {
            "email": "user1@test.com",
            "username": "user1",
            "deptId": depts["A"],
            "roleId": roles["SuperManager"],
        },
        {
            "email": "user2@test.com",
            "username": "user2",
            "deptId": depts["B"],
            "roleId": roles["AreaManager"],
        },
        {
            "email": "user3@test.com",
            "username": "user3",
            "deptId": depts["C"],
            "roleId": roles["CustomViewer"],
        },
        {
            "email": "user4@test.com",
            "username": "user4",
            "deptId": depts["D"],
            "roleId": roles["AreaManager"],
        },
        {
            "email": "user5@test.com",
            "username": "user5",
            "deptId": depts["E"],
            "roleId": None,
        },
    ]

    for cfg in user_configs:
        cfg["password"] = "password"
        cfg["active"] = True
        resp = admin.post("/sys/user", cfg)
        if resp.status_code != 200 or resp.json().get("code") != 200:
            print(f"  FAILED to create user {cfg['email']}: {resp.text}")
        else:
            print(f"  Created user: {cfg['email']}")

    # 3. Verification Phase
    print("\n3. Verifying Permissions and Data Scope...")
    results = []

    for cfg in user_configs:
        print(f"\n--- Testing User: {cfg['username']} ({cfg['email']}) ---")
        try:
            client = LandfunClient(cfg["email"], "password")

            # 3.1 Verify Menu/API Authorization
            # Attempt to list roles (most users shouldn't be able to)
            role_resp = client.get("/sys/role")
            has_role_list = (
                role_resp.status_code == 200 and role_resp.json().get("code") == 200
            )

            # 3.2 Verify Data Scope (counting visible depts)
            dept_resp = client.get("/sys/dept/list", params={"size": 100})
            if dept_resp.status_code == 200 and dept_resp.json().get("code") == 200:
                visible_dept_names = [
                    d["name"] for d in dept_resp.json()["data"]["rows"]
                ]
                visible_count = len(visible_dept_names)
            else:
                visible_dept_names = []
                visible_count = 0

            results.append(
                {
                    "user": cfg["username"],
                    "roleId": cfg.get("roleId"),
                    "can_list_roles": has_role_list,
                    "visible_depts": visible_dept_names,
                }
            )

            print(f"  Can list roles: {has_role_list}")
            print(f"  Visible depts ({visible_count}): {', '.join(visible_dept_names)}")

        except Exception as e:
            print(f"  Error testing user {cfg['email']}: {e}")

    # Final Report
    print("\n=== Final Verification Report ===")
    print(f"{'User':<10} | {'Role List':<10} | {'Visible Depts'}")
    print("-" * 60)
    for r in results:
        depts_str = ", ".join(r["visible_depts"])
        print(f"{r['user']:<10} | {str(r['can_list_roles']):<10} | {depts_str}")

    print("\n3.3 Verifying User Data Scope...")
    for cfg in user_configs:
        try:
            client = LandfunClient(cfg["email"], "password")
            user_resp = client.get("/sys/user", params={"size": 100})
            if user_resp.status_code == 200 and user_resp.json().get("code") == 200:
                visible_users = [
                    u["username"] for u in user_resp.json()["data"]["rows"]
                ]
                print(
                    f"  User {cfg['username']} visible users ({len(visible_users)}): {', '.join(visible_users)}"
                )
            else:
                print(f"  User {cfg['username']} failed to get users: {user_resp.text}")
        except Exception as e:
            print(f"  Error getting users for {cfg['username']}: {e}")


if __name__ == "__main__":
    main()
