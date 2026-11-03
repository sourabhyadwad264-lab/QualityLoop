import { useState } from "react";
import { api } from "../api.js";

export default function Login({ onLogin }) {
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({ username: "", password: "", fullName: "", email: "" });
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const change = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const submit = async () => {
    try {
      setError("");
      if (mode === "login") {
        onLogin(await api.login({ username: form.username, password: form.password }));
      } else {
        await api.register(form);
        setMode("login");
        setNotice("Account created. Log in now. An Admin can raise your role later.");
        setForm({ ...form, password: "" });
      }
    } catch (e) {
      setNotice("");
      setError(e.message);
    }
  };

  return (
    <div className="login-wrap">
      <div className="panel">
        <h1 className="h4 mb-1">QualityLoop</h1>
        <p className="text-secondary">{mode === "login" ? "Log in to continue" : "Create your account"}</p>
        {notice && <div className="alert alert-success">{notice}</div>}
        {error && <div className="alert alert-danger">{error}</div>}

        {mode === "register" && (
          <>
            <div className="mb-3">
              <label className="form-label" htmlFor="fullName">Full name</label>
              <input id="fullName" className="form-control" name="fullName" value={form.fullName} onChange={change} />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="email">Email</label>
              <input id="email" className="form-control" name="email" value={form.email} onChange={change} />
            </div>
          </>
        )}
        <div className="mb-3">
          <label className="form-label" htmlFor="username">Username</label>
          <input id="username" className="form-control" name="username" value={form.username} onChange={change} />
        </div>
        <div className="mb-3">
          <label className="form-label" htmlFor="password">Password</label>
          <input id="password" type="password" className="form-control" name="password" value={form.password}
            onChange={change} onKeyDown={(e) => e.key === "Enter" && submit()} />
        </div>
        <button className="btn btn-success w-100 mb-3" onClick={submit}>
          {mode === "login" ? "Log in" : "Create account"}
        </button>
        <button className="btn btn-link p-0" onClick={() => { setMode(mode === "login" ? "register" : "login"); setError(""); setNotice(""); }}>
          {mode === "login" ? "New here? Create an account" : "Already have an account? Log in"}
        </button>
      </div>
    </div>
  );
}
