import { useState } from "react";
import { api } from "../api.js";

// Three ways to send a report: printable page (PDF), email, WhatsApp.
export default function ShareBox({ defect }) {
  const [email, setEmail] = useState("");
  const [note, setNote] = useState("");
  const [phone, setPhone] = useState("");
  const [msg, setMsg] = useState("");
  const [error, setError] = useState("");

  const run = async (fn) => {
    try {
      setError("");
      setMsg("");
      await fn();
    } catch (e) {
      setError(e.message);
    }
  };

  const openReport = () => run(async () => {
    const s = await api.share(defect.id);
    window.open(s.url, "_blank");
  });

  const copyLink = () => run(async () => {
    const s = await api.share(defect.id);
    await navigator.clipboard.writeText(s.url);
    setMsg("Link copied. It works for 7 days.");
  });

  const sendEmail = () => run(async () => {
    await api.emailReport(defect.id, { to: email, message: note });
    setMsg("Report emailed to " + email);
  });

  const sendWhatsApp = () => run(async () => {
    const digits = phone.replace(/\D/g, "");
    if (!digits) throw new Error("Enter the number with country code, for example 919876543210");
    const s = await api.share(defect.id);
    const lines = [
      "Defect report #" + defect.id + ": " + defect.title,
      "Part: " + defect.partNumber + (defect.partName ? " (" + defect.partName + ")" : ""),
      "Status: " + defect.status.replace("_", " "),
      defect.rootCause ? "Root cause: " + defect.rootCause : "Root cause: under investigation",
      "Corrective actions: " + defect.actions.length,
      "Full report: " + s.url,
    ];
    window.open("https://wa.me/" + digits + "?text=" + encodeURIComponent(lines.join("\n")), "_blank");
  });

  return (
    <div className="share-box mb-4 no-print">
      <h3 className="h6">Send this report</h3>
      {error && <div className="alert alert-danger py-1">{error}</div>}
      {msg && <div className="alert alert-success py-1">{msg}</div>}

      <div className="mb-3">
        <button className="btn btn-sm btn-outline-dark me-2" onClick={openReport}>Open printable report (PDF)</button>
        <button className="btn btn-sm btn-outline-dark" onClick={copyLink}>Copy link</button>
      </div>

      <div className="row g-2 mb-2">
        <div className="col-md-5"><input className="form-control form-control-sm" placeholder="Customer email" value={email} onChange={(e) => setEmail(e.target.value)} /></div>
        <div className="col-md-5"><input className="form-control form-control-sm" placeholder="Short note (optional)" value={note} onChange={(e) => setNote(e.target.value)} /></div>
        <div className="col-md-2"><button className="btn btn-sm btn-success w-100" onClick={sendEmail}>Email</button></div>
      </div>
      <div className="row g-2">
        <div className="col-md-10"><input className="form-control form-control-sm" placeholder="WhatsApp number with country code, e.g. 919876543210" value={phone} onChange={(e) => setPhone(e.target.value)} /></div>
        <div className="col-md-2"><button className="btn btn-sm btn-success w-100" onClick={sendWhatsApp}>WhatsApp</button></div>
      </div>
    </div>
  );
}
