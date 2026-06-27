/** Expected vs Received progress bar with color thresholds. */
export default function ProgressBar({ received, expected, percent }) {
  const pct =
    percent != null
      ? percent
      : expected > 0
      ? Math.min(100, (received / expected) * 100)
      : 0;

  let cls = 'bar-low';
  if (pct >= 100) cls = 'bar-done';
  else if (pct >= 50) cls = 'bar-mid';

  return (
    <div className="progress">
      <div className="progress-track">
        <div className={`progress-fill ${cls}`} style={{ width: `${Math.min(100, pct)}%` }} />
      </div>
      <span className="progress-label">
        {received} / {expected} ({pct.toFixed(0)}%)
      </span>
    </div>
  );
}
