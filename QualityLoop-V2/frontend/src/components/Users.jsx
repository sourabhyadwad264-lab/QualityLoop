import { useEffect, useState } from "react";
import { api } from "../api.js";

const ROLES = [
  ["QUALITY_ENGINEER", "Quality Engineer"],
  ["QUALITY_HEAD", "Quality Head"],
  ["ADMIN", "Admin"],
];

export default function Users({ me }) {
  const [users, setUsers] = useState([]);
  const [error, setError] = useState("");

  const load = () => api.users().then(setUsers).catch((e) => setError(e.message));
  useEffect(() => { load(); }, []);

  const save = async (u, changes) => {
    try {
      setError("");
      await api.updateUser(u.id, { role: u.role, active: u.active, managerId: u.managerId, ...changes });
      await load();
    } catch (e) {
      setError(e.message);
    }
  };

  const managers = users.filter((u) => u.role !== "QUALITY_ENGINEER" && u.active);

  return (
    <div className="panel">
      <h2 className="h5">Users and roles</h2>
      <p className="text-secondary">New accounts start as Quality Engineer. Change the role, choose who they report to, or switch an account off.</p>
      {error && <div className="alert alert-danger">{error}</div>}
      <div className="table-responsive">
        <table className="table align-middle">
          <thead><tr><th>Name</th><th>Username</th><th>Role</th><th>Reports to</th><th>Active</th></tr></thead>
          <tbody>
            {users.map((u) => {
              const self = u.username === me.username;
              return (
                <tr key={u.id}>
                  <td>{u.fullName}<br /><small className="text-secondary">{u.email}</small></td>
                  <td>{u.username}</td>
                  <td>
                    <select className="form-select form-select-sm" value={u.role} disabled={self}
                      onChange={(e) => save(u, { role: e.target.value })}>
                      {ROLES.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
                    </select>
                  </td>
                  <td>
                    <select className="form-select form-select-sm" value={u.managerId || ""} disabled={self}
                      onChange={(e) => save(u, { managerId: e.target.value ? Number(e.target.value) : null })}>
                      <option value="">Nobody</option>
                      {managers.filter((m) => m.id !== u.id).map((m) => <option key={m.id} value={m.id}>{m.fullName}</option>)}
                    </select>
                  </td>
                  <td>
                    <input type="checkbox" className="form-check-input" checked={u.active} disabled={self}
                      onChange={(e) => save(u, { active: e.target.checked })} />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
