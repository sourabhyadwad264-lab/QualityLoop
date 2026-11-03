import { useEffect, useState } from "react";
import { api } from "../api.js";

const STAGES = ["OPEN", "IN_PROGRESS", "CLOSED"];

export default function DefectDetail({ id, onChanged }) {
  const [defect, setDefect] = useState(null);
  const [audit, setAudit] = useState([]);
  const [error, setError] = useState("");
  const [rootCause, setRootCause] = useState("");
  const [containment, setContainment] = useState("");
  const [action, setAction] = useState({ description: "", owner: "", dueDate: "" });

  const refresh = async () => {
    const d = await api.get(id);
    setDefect(d);
    setRootCause(d.rootCause || "");
    setContainment(d.containmentAction || "");
    setAudit(await api.audit(id));
    onChanged();
  };

  useEffect(() => { refresh().catch((e) => setError(e.message)); }, [id]);

  // Runs an API call, then reloads. Any backend rule message is shown to the user.
  const run = async (fn) => {
    try {
      setError("");
      await fn();
      await refresh();
    } catch (e) {
      setError(e.message);
    }
  };

  if (!defect) return <div className="panel">Loading...</div>;
  const closed = defect.status === "CLOSED";

  const saveAnalysis = () =>
    run(() => api.update(id, {
      title: defect.title, description: defect.description, severity: defect.severity,
      reportedBy: defect.reportedBy, containmentAction: containment, rootCause,
    }));

  const addAction = () =>
    run(async () => {
      await api.addAction(id, { ...action, dueDate: action.dueDate || null });
      setAction({ description: "", owner: "", dueDate: "" });
    });

  return (
    <div className="panel">
      <h2 className="h5">#{defect.id} {defect.title}</h2>
      <p className="text-secondary">{defect.description}</p>

      <div className="stage">
        {STAGES.map((s) => (
          <span key={s} className={s === defect.status ? "on" : ""}>{s.replace("_", " ")}</span>
        ))}
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      {!closed && (
        <div className="mb-3">
          {defect.status === "OPEN" && (
            <button className="btn btn-outline-primary btn-sm me-2" onClick={() => run(() => api.setStatus(id, "IN_PROGRESS"))}>Start work</button>
          )}
          <button className="btn btn-success btn-sm" onClick={() => run(() => api.setStatus(id, "CLOSED"))}>Close defect</button>
        </div>
      )}

      <h3 className="h6">Containment and root cause</h3>
      <textarea className="form-control mb-2" rows="2" placeholder="Containment action" value={containment} disabled={closed} onChange={(e) => setContainment(e.target.value)} />
      <textarea className="form-control mb-2" rows="2" placeholder="Root cause" value={rootCause} disabled={closed} onChange={(e) => setRootCause(e.target.value)} />
      {!closed && <button className="btn btn-outline-secondary btn-sm mb-4" onClick={saveAnalysis}>Save analysis</button>}

      <h3 className="h6 mt-3">Corrective actions</h3>
      <table className="table table-sm">
        <thead><tr><th>Action</th><th>Owner</th><th>Due</th><th>Status</th><th></th></tr></thead>
        <tbody>
          {defect.actions.length === 0 && <tr><td colSpan="5" className="text-secondary">No corrective actions yet.</td></tr>}
          {defect.actions.map((a) => (
            <tr key={a.id}>
              <td>{a.description}</td>
              <td>{a.owner}</td>
              <td>{a.dueDate}</td>
              <td>{a.status}</td>
              <td>
                {a.status === "PENDING" && !closed && (
                  <button className="btn btn-sm btn-outline-success" onClick={() => run(() => api.completeAction(id, a.id))}>Mark done</button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {!closed && (
        <div className="row g-2 mb-4">
          <div className="col-md-5"><input className="form-control form-control-sm" placeholder="New action" value={action.description} onChange={(e) => setAction({ ...action, description: e.target.value })} /></div>
          <div className="col-md-3"><input className="form-control form-control-sm" placeholder="Owner" value={action.owner} onChange={(e) => setAction({ ...action, owner: e.target.value })} /></div>
          <div className="col-md-3"><input type="date" className="form-control form-control-sm" value={action.dueDate} onChange={(e) => setAction({ ...action, dueDate: e.target.value })} /></div>
          <div className="col-md-1"><button className="btn btn-sm btn-success" onClick={addAction}>Add</button></div>
        </div>
      )}

      <h3 className="h6">Audit history</h3>
      <ul className="audit p-0">
        {audit.map((a) => (
          <li key={a.id}><strong>{a.event.replace("_", " ")}</strong> <small className="text-secondary">{a.at.replace("T", " ").slice(0, 16)}</small><br /><small>{a.detail}</small></li>
        ))}
      </ul>
    </div>
  );
}
