import { useState } from "react";
import { api } from "../api.js";

const today = () => new Date().toISOString().slice(0, 10);

const empty = {
  title: "", description: "", partNumber: "", partName: "", source: "CUSTOMER_COMPLAINT",
  quantityAffected: "", detectedOn: today(), detectedBy: "", severity: "MEDIUM",
  containmentAction: "", rootCause: "", remarks: "",
};

export default function DefectForm({ onSaved, onCancel }) {
  const [form, setForm] = useState(empty);
  const [error, setError] = useState("");

  const change = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const submit = async () => {
    try {
      // Empty boxes must be sent as null, not as an empty string
      const payload = {
        ...form,
        source: form.source || null,
        detectedOn: form.detectedOn || null,
        quantityAffected: form.quantityAffected === "" ? null : Number(form.quantityAffected),
      };
      onSaved(await api.create(payload));
    } catch (e) {
      setError(e.message);
    }
  };

  const field = (label, name, props = {}) => (
    <div className={props.col || "col-12"}>
      <label className="form-label" htmlFor={name}>{label}</label>
      {props.area ? (
        <textarea id={name} className="form-control" name={name} rows={props.rows || 2} value={form[name]} onChange={change} />
      ) : (
        <input id={name} className="form-control" name={name} type={props.type || "text"} value={form[name]} onChange={change} />
      )}
    </div>
  );

  return (
    <div className="panel">
      <h2 className="h5 mb-3">Report a defect</h2>
      {error && <div className="alert alert-danger">{error}</div>}
      <div className="row g-3">
        {field("Title", "title")}
        {field("Part number", "partNumber", { col: "col-md-6" })}
        {field("Part name", "partName", { col: "col-md-6" })}
        {field("What went wrong", "description", { area: true, rows: 3 })}
        <div className="col-md-6">
          <label className="form-label" htmlFor="source">Where it was found</label>
          <select id="source" className="form-select" name="source" value={form.source} onChange={change}>
            <option value="CUSTOMER_COMPLAINT">Customer complaint</option>
            <option value="INTERNAL_INSPECTION">Internal inspection</option>
            <option value="PPAP_SAMPLE_REJECTION">PPAP sample rejection</option>
          </select>
        </div>
        <div className="col-md-6">
          <label className="form-label" htmlFor="severity">Severity</label>
          <select id="severity" className="form-select" name="severity" value={form.severity} onChange={change}>
            <option>LOW</option><option>MEDIUM</option><option>HIGH</option><option>CRITICAL</option>
          </select>
        </div>
        {field("Detected on", "detectedOn", { type: "date", col: "col-md-4" })}
        {field("Detected by", "detectedBy", { col: "col-md-4" })}
        {field("Quantity affected", "quantityAffected", { type: "number", col: "col-md-4" })}
        {field("Containment action (what stops it spreading now)", "containmentAction", { area: true })}
        {field("Root cause (can be filled in later)", "rootCause", { area: true })}
        {field("Remarks (internal, not shown to customers)", "remarks", { area: true })}
      </div>
      <div className="mt-3">
        <button className="btn btn-success me-2" onClick={submit}>Save defect</button>
        <button className="btn btn-outline-secondary" onClick={onCancel}>Cancel</button>
      </div>
    </div>
  );
}
