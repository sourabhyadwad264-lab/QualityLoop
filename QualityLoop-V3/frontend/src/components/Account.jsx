import { useState } from "react";
import { api } from "../api.js";

const STRONG = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,60}$/;

export default function Account({ user, onPasswordChanged }) {
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [again, setAgain] = useState("");
  const [error, setError] = useState("");

  const submit = async () => {
    try {
      setError("");
      if (!STRONG.test(next)) throw new Error("New password needs 8 or more characters with an upper case letter, a lower case letter and a number");
      if (next !== again) throw new Error("The two new passwords do not match");
      await api.changePassword({ currentPassword: current, newPassword: next });
      onPasswordChanged();
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <div className="panel" style={{ maxWidth: 520 }}>
      <h2 className="h5">{user.fullName}</h2>
      <p className="text-secondary">Signed in as {user.username}</p>
      <h3 className="h6 mt-4">Change password</h3>
      <p className="text-secondary">After changing it, you are logged out everywhere and must log in again.</p>
      {error && <div className="alert alert-danger">{error}</div>}
      <div className="mb-3">
        <label className="form-label" htmlFor="cur">Current password</label>
        <input id="cur" type="password" className="form-control" value={current} onChange={(e) => setCurrent(e.target.value)} />
      </div>
      <div className="mb-3">
        <label className="form-label" htmlFor="new">New password</label>
        <input id="new" type="password" className="form-control" value={next} onChange={(e) => setNext(e.target.value)} />
      </div>
      <div className="mb-3">
        <label className="form-label" htmlFor="again">New password again</label>
        <input id="again" type="password" className="form-control" value={again} onChange={(e) => setAgain(e.target.value)} />
      </div>
      <button className="btn btn-success" onClick={submit}>Change password</button>
    </div>
  );
}
