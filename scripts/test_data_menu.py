import requests
import json
import sys

BASE_URL = "http://localhost:8080"
ECHO_PREFIX = "[PERM-TEST]"


class LandfunTester:
    def __init__(self):
        self.session = requests.Session()
        self.headers = {"Content-Type": "application/json"}
        self.token = None

    def login(self, username, password):
        url = f"{BASE_URL}/auth/login"
        payload = {"email": username, "password": password}
        try:
            response = self.session.post(url, json=payload, headers=self.headers)
            if response.status_code == 200:
                data = response.json()
                self.token = data.get("data", {}).get("token")
                if self.token:
                    self.headers["Authorization"] = self.token
                    return True
            print(f"Login failed for {username}: {response.text}")
            return False
        except Exception as e:
            print(f"Login error: {str(e)}")
            return False

    def create_dept(self, name, parent_id=None):
        payload = {"name": name, "parentId": parent_id or 0}
        resp = self.session.post(
            f"{BASE_URL}/sys/dept/create", json=payload, headers=self.headers
        )
        if resp.status_code == 200:
            res_json = resp.json()
            if res_json.get("code") == 200:
                print(f"{ECHO_PREFIX} Created Dept '{name}' ID: {res_json.get('data')}")
                return res_json.get("data")
        print(f"{ECHO_PREFIX} Failed to create dept: {resp.text}")
        return None

    def get_dept_tree(self):
        resp = self.session.get(f"{BASE_URL}/sys/dept/tree", headers=self.headers)
        if resp.status_code == 200:
            return resp.json().get("data", [])
        return None

    def create_role(self, name, code, menu_ids, data_scope):
        # dataScope: ALL, DEPT_SAME, DEPT_RECURSIVE, SELF
        payload = {
            "name": name,
            "code": code,
            "description": f"Role for {name}",
            "menuIds": menu_ids,
            "dataScope": data_scope,
        }
        resp = self.session.post(
            f"{BASE_URL}/sys/role", json=payload, headers=self.headers
        )
        if resp.status_code == 200:
            if resp.json().get("code") == 200:
                print(f"{ECHO_PREFIX} Created Role '{name}' with scope {data_scope}")
                return resp.json().get("data")
        print(f"{ECHO_PREFIX} Failed create role: {resp.text}")
        return None

    def create_user(self, username, email, role_id, dept_id):
        payload = {
            "username": username,
            "email": email,
            "password": "password",
            "active": True,
            "roleId": role_id,
            "deptId": dept_id,
        }
        resp = self.session.post(
            f"{BASE_URL}/sys/user", json=payload, headers=self.headers
        )
        if resp.status_code == 200:
            if resp.json().get("code") == 200:
                print(f"{ECHO_PREFIX} Created User '{username}' in Dept {dept_id}")
                return resp.json().get("data")
        print(f"Failed create user: {resp.text}")
        return None

    def get_menus(self):
        resp = self.session.get(f"{BASE_URL}/sys/menu/tree", headers=self.headers)
        if resp.status_code == 200:
            return resp.json().get("data", [])
        return []


def count_nodes(nodes):
    count = 0
    if not nodes:
        return 0
    for n in nodes:
        count += 1
        count += count_nodes(n.get("children", []))
    return count


def collect_ids(nodes):
    ids = set()
    if not nodes:
        return ids
    for n in nodes:
        ids.add(n.get("id"))
        ids.update(collect_ids(n.get("children", [])))
    return ids


def run_verify():
    admin = LandfunTester()
    if not admin.login("admin@landfun.com", "password"):
        sys.exit(1)

    print("\n--- 1. Setup Data ---\n")
    # 1. Create Dept Structure: Root -> Sub
    root_id = admin.create_dept("ScopeRoot")
    sub_id = admin.create_dept("ScopeSub", root_id)

    # 2. Create Roles
    # Role A: DEPT_SAME (See only own dept)
    # Role B: DEPT_RECURSIVE (See own + children)
    menus = admin.get_menus()
    all_menu_ids = list(collect_ids(menus))
    # Let's give them limited menus just to see if menu filter works
    limited_menu_ids = all_menu_ids[:1] if all_menu_ids else []

    role_same_id = admin.create_role(
        "RoleSame", "role_same", limited_menu_ids, "DEPT_SAME"
    )
    role_recur_id = admin.create_role(
        "RoleRecur", "role_recur", limited_menu_ids, "DEPT_RECURSIVE"
    )

    # 3. Create Users
    # UserSame in ScopeRoot
    user_same_id = admin.create_user(
        "u_same", "u_same@landfun.com", role_same_id, root_id
    )
    # UserRecur in ScopeRoot
    user_recur_id = admin.create_user(
        "u_recur", "u_recur@landfun.com", role_recur_id, root_id
    )

    print("\n--- 2. Verify Data Permissions (Dept Tree) ---\n")

    # Test User Same
    tester_same = LandfunTester()
    tester_same.login("u_same@landfun.com", "password")
    tree_same = tester_same.get_dept_tree()
    ids_same = collect_ids(tree_same)
    print(f"User Same (DEPT_SAME at Root) sees Depts: {ids_same}")
    # Expect: Root ID only (maybe). Or nothing if logic is strict?
    # Logic says: id = userDeptId. So should see Root.

    # Test User Recur
    tester_recur = LandfunTester()
    tester_recur.login("u_recur@landfun.com", "password")
    tree_recur = tester_recur.get_dept_tree()
    ids_recur = collect_ids(tree_recur)
    print(f"User Recur (DEPT_RECURSIVE at Root) sees Depts: {ids_recur}")
    # Expect: Root and Sub.

    # Verification
    # Note: DeptFilter.java logic:
    # DEPT_SAME -> args.where(table.id.eq(userDeptId))
    # DEPT_RECURSIVE -> args.where(table.id.in(getSubDeptIds))

    pass_same = (root_id in ids_same) and (sub_id not in ids_same)
    pass_recur = (root_id in ids_recur) and (sub_id in ids_recur)

    print(f"Result Same: {'PASS' if pass_same else 'FAIL'}")
    print(f"Result Recur: {'PASS' if pass_recur else 'FAIL'}")

    print("\n--- 3. Verify Menu Permissions ---\n")
    # Admin sees all
    admin_count = count_nodes(menus)
    print(f"Admin Menu Count: {admin_count}")

    # User Same (Limited Menus assigned to Role)
    user_menu_count = count_nodes(tester_same.get_menus())
    print(f"UserSame Menu Count: {user_menu_count}")

    if user_menu_count < admin_count and user_menu_count == count_nodes(
        [{"children": []} for _ in limited_menu_ids]
    ):
        # Rough check
        print("Menu Permission: PASS (Count is restricted)")
    elif user_menu_count == admin_count:
        print("Menu Permission: FAIL (User sees ALL menus, endpoint not filtered)")
    else:
        print("Menu Permission: UNKNOWN")


if __name__ == "__main__":
    run_verify()
