import { useEffect, useState } from "react";
import { api } from "../api.js";

export default function Dashboard() {
  const [s, setS] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => { api.summary().then(setS).catch((e) => setError(e.message)); }, []);

  if (error) return <div className="alert alert-danger">{error}</div>;
  if (!s) return <div className="panel">Loading...</div>;

  const stat = (label, value, color) => (
    <div className="col-6 col-md-3"><div className="stat"><div className="num" style={{ color }}>{value}</div><div className="text-secondary">{label}</div></div></div>
  );

  return (
    <>
      <div className="row g-3 mb-4">
        {stat("Total defects", s.total)}
        {stat("Open", s.open, "#c05621")}
        {stat("In progress", s.inProgress, "#2b6cb0")}
        {stat("Closed", s.closed, "#0b6e4f")}
      </div>

      <div className="row g-4">
        <div className="col-md-6">
          <div className="panel h-100">
            <h2 className="h6">By severity</h2>
            {Object.entries(s.bySeverity).map(([k, v]) => (
              <div key={k} className="d-flex justify-content-between border-bottom py-1"><span>{k}</span><strong>{v}</strong></div>
            ))}
          </div>
        </div>
        <div className="col-md-6">
          <div className="panel h-100">
            <h2 className="h6">Parts with most defects</h2>
            {s.topParts.length === 0 && <p className="text-secondary m-0">No data yet.</p>}
            {s.topParts.map((p) => (
              <div key={p.partNumber} className="d-flex justify-content-between border-bottom py-1"><span>{p.partNumber}</span><strong>{p.count}</strong></div>
            ))}
          </div>
        </div>
      </div>

      <div className="panel mt-4">
        <h2 className="h6">Overdue corrective actions</h2>
        {s.overdue.length === 0 ? (
          <p className="text-secondary m-0">Nothing is overdue.</p>
        ) : (
          <table className="table table-sm m-0">
            <thead><tr><th>Defect</th><th>Action</th><th>Owner</th><th>Was due</th></tr></thead>
            <tbody>
              {s.overdue.map((o, i) => (
                <tr key={i}><td>#{o.defectId} {o.defectTitle}</td><td>{o.action}</td><td>{o.owner}</td><td className="text-danger">{o.dueDate}</td></tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  );
}
