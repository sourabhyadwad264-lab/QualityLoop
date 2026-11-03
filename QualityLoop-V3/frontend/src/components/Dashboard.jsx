import { useEffect, useState } from "react";
import { api } from "../api.js";

const SEV_COLOR = { LOW: "var(--sev-low)", MEDIUM: "var(--sev-medium)", HIGH: "var(--sev-high)", CRITICAL: "var(--sev-critical)" };

export default function Dashboard() {
  const [s, setS] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => { api.summary().then(setS).catch((e) => setError(e.message)); }, []);

  if (error) return <div className="alert alert-danger">{error}</div>;
  if (!s) return <div className="panel">Loading...</div>;

  const pct = (n) => (s.total === 0 ? 0 : (n / s.total) * 100);
  const a = pct(s.open);
  const b = a + pct(s.inProgress);
  const donut = s.total === 0
    ? "var(--surface-2)"
    : `conic-gradient(var(--c-open) 0 ${a}%, var(--c-prog) ${a}% ${b}%, var(--c-closed) ${b}% 100%)`;

  const maxSev = Math.max(1, ...Object.values(s.bySeverity));
  const maxPart = Math.max(1, ...s.topParts.map((p) => p.count));
  const maxMonth = Math.max(1, ...s.byMonth.map((m) => m.count));

  const stat = (label, value, color) => (
    <div className="col-6 col-lg-3">
      <div className="stat"><div className="num" style={{ color }}>{value}</div><div className="lbl">{label}</div></div>
    </div>
  );

  return (
    <>
      <div className="row g-3 mb-3">
        {stat("Total defects", s.total)}
        {stat("Open", s.open, "var(--c-open)")}
        {stat("Closed", s.closed, "var(--c-closed)")}
        {stat("Average days to close", s.avgCloseDays === null ? "-" : s.avgCloseDays)}
      </div>

      <div className="row g-3 mb-3">
        <div className="col-lg-5">
          <div className="panel h-100">
            <h2 className="h6 mb-3">Where defects stand</h2>
            <div className="d-flex align-items-center gap-4">
              <div className="donut" style={{ background: donut }}>
                <div className="donut-hole"><div><strong>{s.total}</strong><br /><span>defects</span></div></div>
              </div>
              <div>
                <div className="legend"><span className="dot" style={{ background: "var(--c-open)" }} />Open <strong>{s.open}</strong></div>
                <div className="legend"><span className="dot" style={{ background: "var(--c-prog)" }} />In progress <strong>{s.inProgress}</strong></div>
                <div className="legend"><span className="dot" style={{ background: "var(--c-closed)" }} />Closed <strong>{s.closed}</strong></div>
              </div>
            </div>
          </div>
        </div>
        <div className="col-lg-7">
          <div className="panel h-100">
            <h2 className="h6 mb-3">Reported per month</h2>
            <div className="months">
              {s.byMonth.map((m) => (
                <div className="col" key={m.month}>
                  <span className="val">{m.count}</span>
                  <div className="bar" style={{ height: (m.count / maxMonth) * 100 + "px" }} />
                  <span className="mt-1">{m.month.slice(5)}/{m.month.slice(2, 4)}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className="row g-3 mb-3">
        <div className="col-lg-6">
          <div className="panel h-100">
            <h2 className="h6 mb-3">By severity</h2>
            {Object.entries(s.bySeverity).map(([k, v]) => (
              <div className="hbar" key={k}>
                <span>{k}</span>
                <div className="track"><div className="fill" style={{ width: (v / maxSev) * 100 + "%", background: SEV_COLOR[k] }} /></div>
                <strong>{v}</strong>
              </div>
            ))}
          </div>
        </div>
        <div className="col-lg-6">
          <div className="panel h-100">
            <h2 className="h6 mb-3">Parts with most defects</h2>
            {s.topParts.length === 0 && <p className="text-secondary m-0">No data yet.</p>}
            {s.topParts.map((p) => (
              <div className="hbar" key={p.partNumber}>
                <span className="text-truncate">{p.partNumber}</span>
                <div className="track"><div className="fill" style={{ width: (p.count / maxPart) * 100 + "%", background: "var(--accent)" }} /></div>
                <strong>{p.count}</strong>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="panel">
        <h2 className="h6 mb-3">Overdue corrective actions</h2>
        {s.overdue.length === 0 ? (
          <p className="text-secondary m-0">Nothing is overdue.</p>
        ) : (
          <div className="table-responsive">
            <table className="table table-sm m-0">
              <thead><tr><th>Defect</th><th>Action</th><th>Owner</th><th>Was due</th></tr></thead>
              <tbody>
                {s.overdue.map((o, i) => (
                  <tr key={i}><td>#{o.defectId} {o.defectTitle}</td><td>{o.action}</td><td>{o.owner}</td><td className="text-danger">{o.dueDate}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
}
