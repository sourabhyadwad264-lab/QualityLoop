import { useState } from "react";
import { api } from "../api.js";

const empty = { title: "", description: "", severity: "MEDIUM", reportedBy: "", containmentAction: "", rootCause: "" };

export default function DefectForm({ onSaved, onCancel }) {
  const [form, setForm] = useState(empty);
  const [error, setError] = useState("");

  const change = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const submit = async () => {
    try {
      onSaved(await api.create(form));
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <div className="panel">
      <h2 className="h5 mb-3">Report a defect</h2>
      {error && <div className="alert alert-danger">{error}</div>}
      <div className="mb-3">
        <label className="form-label" htmlFor="title">Title</label>
        <input id="title" className="form-control" name="title" value={form.title} onChange={change} />
      </div>
      <div className="mb-3">
        <label className="form-label" htmlFor="description">What went wrong</label>
        <textarea id="description" className="form-control" name="description" rows="3" value={form.description} onChange={change} />
      </div>
      <div className="row">
        <div className="col-md-6 mb-3">
          <label className="form-label" htmlFor="severity">Severity</label>
          <select id="severity" className="form-select" name="severity" value={form.severity} onChange={change}>
            <option>LOW</option><option>MEDIUM</option><option>HIGH</option><option>CRITICAL</option>
          </select>
        </div>
        <div className="col-md-6 mb-3">
          <label className="form-label" htmlFor="reportedBy">Reported by</label>
          <input id="reportedBy" className="form-control" name="reportedBy" value={form.reportedBy} onChange={change} />
        </div>
      </div>
      <div className="mb-3">
        <label className="form-label" htmlFor="containmentAction">Containment action (what stops it spreading now)</label>
        <textarea id="containmentAction" className="form-control" name="containmentAction" rows="2" value={form.containmentAction} onChange={change} />
      </div>
      <div className="mb-3">
        <label className="form-label" htmlFor="rootCause">Root cause (can be filled in later)</label>
        <textarea id="rootCause" className="form-control" name="rootCause" rows="2" value={form.rootCause} onChange={change} />
      </div>
      <button className="btn btn-success me-2" onClick={submit}>Save defect</button>
      <button className="btn btn-outline-secondary" onClick={onCancel}>Cancel</button>
    </div>
  );
}
