import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "FreshTrack — Inbound Receiving System" },
      {
        name: "description",
        content:
          "FreshTrack: Java Spring Boot + React + MySQL inbound fruit & vegetable receiving system with JWT auth, RBAC, invoice ingestion, scan-to-receive, and reconciliation.",
      },
      { property: "og:title", content: "FreshTrack — Inbound Receiving System" },
      {
        property: "og:description",
        content:
          "Full-stack receiving platform with JWT auth, RBAC, scan-to-receive, and reconciliation reporting.",
      },
    ],
  }),
  component: Index,
});

const features = [
  ["Role-based auth", "Central Admin (global) and Hub User (assigned hubs) with JWT."],
  ["Invoice ingestion", "CSV/Excel upload with validation and per-row results."],
  ["Scan-to-receive", "Barcode + camera scanning, +1 per scan, real-time progress."],
  ["Warehouse mapping", "Map users to one or more warehouses."],
  ["Reconciliation", "Expected vs received variance, CSV/Excel export."],
  ["Audit trail", "Append-only log of every scan and manual action."],
];

const endpoints: [string, string, string][] = [
  ["POST", "/api/auth/login", "Authenticate, returns JWT"],
  ["POST", "/api/invoices/upload", "Upload CSV/Excel invoices (admin)"],
  ["POST", "/api/receiving/scan", "+1 receive by SKU"],
  ["GET", "/api/reports/reconciliation", "Expected vs received variance"],
  ["GET", "/api/audit", "Audit trail (admin)"],
];

function Index() {
  return (
    <div className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-5xl px-6 py-12">
        <header className="mb-10">
          <div className="inline-flex items-center gap-2 rounded-full border border-border px-3 py-1 text-sm text-muted-foreground">
            <span>❄</span> FreshTrack
          </div>
          <h1 className="mt-4 text-4xl font-bold tracking-tight">
            Inbound Fruit &amp; Vegetable Receiving System
          </h1>
          <p className="mt-3 max-w-2xl text-muted-foreground">
            Full-stack project (Java Spring Boot + React + MySQL) generated in{" "}
            <code className="rounded bg-muted px-1.5 py-0.5 text-sm">freshtrack/</code>. The
            backend and frontend are written and build cleanly, but they run on a JVM + MySQL
            stack that this preview cannot execute — run them locally to use the app.
          </p>
        </header>

        <section className="mb-10 rounded-lg border border-border bg-card p-6">
          <h2 className="text-lg font-semibold">Run it locally</h2>
          <ol className="mt-3 space-y-2 text-sm text-muted-foreground">
            <li>
              <strong className="text-foreground">Backend:</strong>{" "}
              <code className="rounded bg-muted px-1.5 py-0.5">cd freshtrack/backend &amp;&amp; mvn spring-boot:run -Dspring-boot.run.profiles=h2</code>{" "}
              (zero-setup H2, runs on :8080)
            </li>
            <li>
              <strong className="text-foreground">Frontend:</strong>{" "}
              <code className="rounded bg-muted px-1.5 py-0.5">cd freshtrack/frontend &amp;&amp; npm install &amp;&amp; npm run dev</code>{" "}
              (runs on :5173)
            </li>
            <li>
              <strong className="text-foreground">Docker:</strong>{" "}
              <code className="rounded bg-muted px-1.5 py-0.5">cd freshtrack &amp;&amp; docker compose up --build</code>
            </li>
            <li>
              Login with <code className="rounded bg-muted px-1.5 py-0.5">admin / admin123</code>{" "}
              (Central Admin) or{" "}
              <code className="rounded bg-muted px-1.5 py-0.5">hubdel / hub123</code> (Hub User).
            </li>
          </ol>
        </section>

        <section className="mb-10">
          <h2 className="text-lg font-semibold">Features</h2>
          <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {features.map(([title, desc]) => (
              <div key={title} className="rounded-lg border border-border bg-card p-4">
                <h3 className="font-medium">{title}</h3>
                <p className="mt-1 text-sm text-muted-foreground">{desc}</p>
              </div>
            ))}
          </div>
        </section>

        <section className="mb-4">
          <h2 className="text-lg font-semibold">Key REST endpoints</h2>
          <div className="mt-4 overflow-hidden rounded-lg border border-border">
            <table className="w-full text-left text-sm">
              <thead className="bg-muted text-muted-foreground">
                <tr>
                  <th className="px-4 py-2 font-medium">Method</th>
                  <th className="px-4 py-2 font-medium">Path</th>
                  <th className="px-4 py-2 font-medium">Description</th>
                </tr>
              </thead>
              <tbody>
                {endpoints.map(([m, p, d]) => (
                  <tr key={p} className="border-t border-border">
                    <td className="px-4 py-2 font-mono text-xs">{m}</td>
                    <td className="px-4 py-2 font-mono text-xs">{p}</td>
                    <td className="px-4 py-2 text-muted-foreground">{d}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <footer className="mt-10 border-t border-border pt-6 text-sm text-muted-foreground">
          See <code className="rounded bg-muted px-1.5 py-0.5">freshtrack/README.md</code> for full
          setup instructions, schema, and sample data.
        </footer>
      </div>
    </div>
  );
}
