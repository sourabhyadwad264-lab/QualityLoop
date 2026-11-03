import { useEffect, useState } from "react";
import { api } from "../api.js";
import ShareBox from "./ShareBox.jsx";

const STAGES = ["OPEN", "IN_PROGRESS", "CLOSED"];

export default function DefectDetail({ id, canManage, onChanged, onDeleted }) {
  const [defect, setDefect] = useState(null);
  const [audit, setAudit] = useState([]);
  const [error, setError] = useState("");
  const [rootCause, setRootCause] = useState("");
  const [containment, setContainment] = useState("");
  const [remarks, setRemarks] = useState("");
  const [action, setAction] = useState({ description: "", owner: "", dueDate: "" });

  const refresh = async () => {
    const d = await api.get(id);
    setDefect(d);
    setRootCause(d.rootCause || "");
    setContainment(d.containmentAction || "");
    setRemarks(d.remarks || "");
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

  if (!defect) return <div className="panel">{error || "Loading..."}</div>;
  const closed = defect.status === "CLOSED";

  const saveAnalysis = () =>
    run(() => api.update(id, {
      title: defect.title, description: defect.description, partNumber: defect.partNumber,
      partName: defect.partName, source: defect.source, quantityAffected: defect.quantityAffected,
      detectedOn: defect.detectedOn, detectedBy: defect.detectedBy, severity: defect.severity,
      containmentAction: containment, rootCause, remarks,
    }));

  const addAction = () =>
    run(async () => {
      await api.addAction(id, { ...action, dueDate: action.dueDate || null });
      setAction({ description: "", owner: "", dueDate: "" });
    });

  const deleteDefect = async () => {
    if (!window.confirm("Delete defect #" + id + "? This cannot be undone.")) return;
    try {
      await api.remove(id);
      onDeleted();
    } catch (e) {
      setError(e.message);
    }
  };

  const today = new Date().toISOString().slice(0, 10);
  const info = (label, value) => (
    <tr><th className="pe-3 text-secondary fw-normal">{label}</th><td>{value || "-"}</td></tr>
  );

  return (
    <div className="panel">
      <div className="d-flex justify-content-between">
        <h2 className="h5">#{defect.id} {defect.title}</h2>
        {canManage && defect.status === "OPEN" && (
          <button className="btn btn-sm btn-outline-danger" onClick={deleteDefect}>Delete</button>
        )}
      </div>
      <p className="text-secondary">{defect.description}</p>

      <table className="table table-sm w-auto mb-3">
        <tbody>
          {info("Part", defect.partNumber + (defect.partName ? " - " + defect.partName : ""))}
          {info("Found through", defect.source && defect.source.replaceAll("_", " ").toLowerCase())}
          {info("Quantity affected", defect.quantityAffected)}
          {info("Detected", (defect.detectedOn || "-") + (defect.detectedBy ? " by " + defect.detectedBy : ""))}
          {info("Severity", defect.severity)}
          {info("Entered by", defect.reportedBy)}
        </tbody>
      </table>

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
          {canManage ? (
            <button className="btn btn-success btn-sm" onClick={() => run(() => api.setStatus(id, "CLOSED"))}>Close defect</button>
          ) : (
            <small className="text-secondary">Only a Quality Head or Admin can close this defect.</small>
          )}
        </div>
      )}

      <h3 className="h6">Containment, root cause and remarks</h3>
      <textarea className="form-control mb-2" rows="2" placeholder="Containment action" value={containment} disabled={closed} onChange={(e) => setContainment(e.target.value)} />
      <textarea className="form-control mb-2" rows="2" placeholder="Root cause" value={rootCause} disabled={closed} onChange={(e) => setRootCause(e.target.value)} />
      <textarea className="form-control mb-2" rows="2" placeholder="Remarks (internal only)" value={remarks} disabled={closed} onChange={(e) => setRemarks(e.target.value)} />
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
              <td className={a.status === "PENDING" && a.dueDate && a.dueDate < today ? "text-danger fw-semibold" : ""}>
                {a.dueDate}{a.status === "PENDING" && a.dueDate && a.dueDate < today ? " (overdue)" : ""}
              </td>
              <td>{a.status}</td>
              <td className="text-nowrap">
                {a.status === "PENDING" && !closed && (
                  <button className="btn btn-sm btn-outline-success me-1" onClick={() => run(() => api.completeAction(id, a.id))}>Mark done</button>
                )}
                {canManage && !closed && (
                  <button className="btn btn-sm btn-outline-danger" onClick={() => run(() => api.removeAction(id, a.id))}>Remove</button>
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

      <ShareBox defect={defect} />

      <h3 className="h6">Audit history</h3>
      <ul className="audit p-0">
        {audit.map((a) => (
          <li key={a.id}>
            <strong>{a.event.replaceAll("_", " ")}</strong>{" "}
            <small className="text-secondary">{a.at.replace("T", " ").slice(0, 16)}{a.performedBy ? " by " + a.performedBy : ""}</small>
            <br /><small>{a.detail}</small>
          </li>
        ))}
      </ul>
    </div>
  );
}
