import { useEffect, useState } from "react";
import { api } from "../api.js";

const REASONS = {
  LOGIN_OK: "Signed in",
  WRONG_PASSWORD: "Wrong password",
  UNKNOWN_USER: "Unknown username",
  ACCOUNT_LOCKED: "Blocked, account locked",
  ACCOUNT_DISABLED: "Account switched off",
};

export default function SecurityLog() {
  const [events, setEvents] = useState([]);
  const [onlyFailed, setOnlyFailed] = useState(false);
  const [error, setError] = useState("");

  const load = () => api.loginEvents().then(setEvents).catch((e) => setError(e.message));
  useEffect(() => { load(); }, []);

  const shown = onlyFailed ? events.filter((e) => !e.success) : events;
  const failed = events.filter((e) => !e.success).length;

  return (
    <div className="panel">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h2 className="h5 m-0">Login attempts</h2>
          <small className="text-secondary">Last 100 attempts. {failed} failed.</small>
        </div>
        <div className="d-flex align-items-center gap-3">
          <label className="form-check-label d-flex align-items-center gap-2">
            <input type="checkbox" className="form-check-input m-0" checked={onlyFailed} onChange={(e) => setOnlyFailed(e.target.checked)} />
            Failed only
          </label>
          <button className="btn btn-sm btn-outline-secondary" onClick={load}>Refresh</button>
        </div>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}
      <div className="table-responsive">
        <table className="table table-sm align-middle">
          <thead><tr><th>When</th><th>Username</th><th>Result</th><th>Address</th></tr></thead>
          <tbody>
            {shown.length === 0 && <tr><td colSpan="4" className="text-secondary">Nothing to show.</td></tr>}
            {shown.map((e) => (
              <tr key={e.id}>
                <td>{e.at.replace("T", " ").slice(0, 19)}</td>
                <td>{e.username}</td>
                <td><span className={"badge-soft " + (e.success ? "badge-good" : "badge-bad")}>{REASONS[e.reason] || e.reason}</span></td>
                <td>{e.ipAddress}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
