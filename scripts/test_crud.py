import requests
import json
import sys

BASE_URL = "http://localhost:8080"
ECHO_PREFIX = "[TEST]"


def print_result(name, success, message=""):
    status = "PASS" if success else "FAIL"
    print(f"{ECHO_PREFIX} {name}: {status} - {message}")


class LandfunTester:
    def __init__(self):
        self.session = requests.Session()
        self.headers = {"Content-Type": "application/json"}
        self.token = None

    def login(self, username, password):
        url = f"{BASE_URL}/auth/login"
        payload = {
            "email": username,
            "password": password,
        }  # LoginReq uses email field for username/email
        try:
            response = self.session.post(url, json=payload, headers=self.headers)
            if response.status_code == 200:
                data = response.json()
                self.token = data.get("data", {}).get("token")
                if self.token:
                    self.headers["Authorization"] = self.token
                    print_result("Login", True, "Token acquired")
                    return True
            print_result(
                "Login", False, f"Status: {response.status_code}, Body: {response.text}"
            )
            return False
        except Exception as e:
            print_result("Login", False, str(e))
            return False

    def test_dept_crud(self):
        print("\n--- Testing Dept CRUD ---")
        dept_id = None

        # Create
        create_payload = {"name": "TestDept", "parentId": 0}  # root
        resp = self.session.post(
            f"{BASE_URL}/sys/dept/create", json=create_payload, headers=self.headers
        )
        if resp.status_code == 200 and resp.json().get("code") == 200:
            dept_id = resp.json().get("data")
            print_result("Dept Create", True, f"ID: {dept_id}")
        else:
            print_result("Dept Create", False, resp.text)
            return

        # Update
        update_payload = {"id": dept_id, "name": "TestDept_Updated", "parentId": 0}
        resp = self.session.post(
            f"{BASE_URL}/sys/dept/update", json=update_payload, headers=self.headers
        )
        if resp.status_code == 200:
            print_result("Dept Update", True)
        else:
            print_result("Dept Update", False, resp.text)

        # Tree (Read)
        resp = self.session.get(f"{BASE_URL}/sys/dept/tree", headers=self.headers)
        if resp.status_code == 200:
            print_result(
                "Dept Tree", True, f"Nodes: {len(resp.json().get('data', []))}"
            )
        else:
            print_result("Dept Tree", False, resp.text)

        # Delete
        delete_payload = {"id": dept_id}
        resp = self.session.post(
            f"{BASE_URL}/sys/dept/delete", json=delete_payload, headers=self.headers
        )
        if resp.status_code == 200:
            print_result("Dept Delete", True)
        else:
            print_result("Dept Delete", False, resp.text)

    def test_menu_crud(self):
        print("\n--- Testing Menu CRUD ---")
        menu_id = None

        # Create
        create_payload = {
            "name": "TestMenu",
            "path": "/test",
            "permission": "sys:test:list",
            "type": "BUTTON",
            "parentId": 0,
        }
        resp = self.session.post(
            f"{BASE_URL}/sys/menu/create", json=create_payload, headers=self.headers
        )
        if resp.status_code == 200 and resp.json().get("code") == 200:
            menu_id = resp.json().get("data")
            print_result("Menu Create", True, f"ID: {menu_id}")
        else:
            print_result("Menu Create", False, resp.text)
            return

        # Update
        update_payload = {
            "id": menu_id,
            "name": "TestMenu_Upd",
            "path": "/test2",
            "permission": "sys:test:list",
            "type": "BUTTON",
            "parentId": 0,
        }
        resp = self.session.post(
            f"{BASE_URL}/sys/menu/update", json=update_payload, headers=self.headers
        )
        if resp.status_code == 200:
            print_result("Menu Update", True)
        else:
            print_result("Menu Update", False, resp.text)

        # Tree
        resp = self.session.get(f"{BASE_URL}/sys/menu/tree", headers=self.headers)
        if resp.status_code == 200:
            print_result("Menu Tree", True)
        else:
            print_result("Menu Tree", False, resp.text)

        # Delete
        delete_payload = {"id": menu_id}
        resp = self.session.post(
            f"{BASE_URL}/sys/menu/delete", json=delete_payload, headers=self.headers
        )
        if resp.status_code == 200:
            print_result("Menu Delete", True)
        else:
            print_result("Menu Delete", False, resp.text)

    def test_role_crud(self):
        print("\n--- Testing Role CRUD ---")
        role_id = None

        # Create
        create_payload = {
            "code": "test_role",
            "name": "Test Role",
            "description": "Desc",
            "menuIds": [],
        }
        resp = self.session.post(
            f"{BASE_URL}/sys/role", json=create_payload, headers=self.headers
        )
        if resp.status_code == 200 and resp.json().get("code") == 200:
            role_id = resp.json().get("data")
            print_result("Role Create", True, f"ID: {role_id}")
        else:
            print_result("Role Create", False, resp.text)
            return

        # Update
        update_payload = {
            "id": role_id,
            "code": "test_role",
            "name": "Test Role Upd",
            "description": "Desc2",
            "menuIds": [],
        }
        resp = self.session.put(
            f"{BASE_URL}/sys/role", json=update_payload, headers=self.headers
        )
        if resp.status_code == 200:
            print_result("Role Update", True)
        else:
            print_result("Role Update", False, resp.text)

        # List
        resp = self.session.get(
            f"{BASE_URL}/sys/role",
            params={"pageIndex": 0, "pageSize": 10},
            headers=self.headers,
        )
        if resp.status_code == 200:
            print_result("Role List", True)
        else:
            print_result("Role List", False, resp.text)

        # Delete
        resp = self.session.delete(
            f"{BASE_URL}/sys/role/{role_id}", headers=self.headers
        )
        if resp.status_code == 200:
            print_result("Role Delete", True)
        else:
            print_result("Role Delete", False, resp.text)

    def test_user_crud(self):
        print("\n--- Testing User CRUD ---")
        user_id = None

        # Create
        create_payload = {
            "username": "apitestuser",
            "email": "apitest@example.com",
            "password": "password123",
            "active": True,
            "roleId": None,
        }
        resp = self.session.post(
            f"{BASE_URL}/sys/user", json=create_payload, headers=self.headers
        )
        if resp.status_code == 200 and resp.json().get("code") == 200:
            user_id = resp.json().get("data")
            print_result("User Create", True, f"ID: {user_id}")
        else:
            print_result("User Create", False, resp.text)
            return

        # Update
        update_payload = {
            "id": user_id,
            "username": "apitestuser",
            "email": "apitest@example.com",
            "password": "password123",
            "active": False,
            "roleId": None,
        }
        resp = self.session.put(
            f"{BASE_URL}/sys/user", json=update_payload, headers=self.headers
        )
        if resp.status_code == 200:
            print_result("User Update", True)
        else:
            print_result("User Update", False, resp.text)

        # List
        resp = self.session.get(
            f"{BASE_URL}/sys/user?pageIndex=0&pageSize=10", headers=self.headers
        )
        if resp.status_code == 200:
            print_result("User List", True)
        else:
            print_result("User List", False, resp.text)

        # Delete
        resp = self.session.delete(
            f"{BASE_URL}/sys/user/{user_id}", headers=self.headers
        )
        if resp.status_code == 200:
            print_result("User Delete", True)
        else:
            print_result("User Delete", False, resp.text)


if __name__ == "__main__":
    tester = LandfunTester()
    if tester.login("admin@landfun.com", "password"):
        pass
    else:
        # Retry with email if username fails
        if not tester.login("admin", "password"):
            sys.exit(1)

    tester.test_dept_crud()
    tester.test_menu_crud()
    tester.test_role_crud()
    tester.test_user_crud()
