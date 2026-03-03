import type { ReferenceItem } from '../types';

interface Props {
  data: string;
}

export function McpReferences({ data }: Props) {
  let references: ReferenceItem[] = [];
  try {
    const parsed = JSON.parse(data);
    references = parsed.references || [];
  } catch {
    return null;
  }

  if (references.length === 0) return null;

  return (
    <div className="mcp-references">
      <div className="mcp-ref-header">
        <span className="mcp-ref-title">Sources & References</span>
        <span className="mcp-ref-count">{references.length}</span>
      </div>
      <div className="mcp-ref-list">
        {references.map((ref, i) => {
          const pct = Math.round((ref.score || 0) * 100);
          const scoreClass = pct >= 75 ? 'high' : pct >= 50 ? 'medium' : 'low';
          return (
            <div key={`${ref.sourceFile}-${ref.chunkIndex}-${i}`} className="mcp-ref-card">
              <div className="mcp-ref-card-header">
                <span className="mcp-ref-card-title">{ref.sourceFile}</span>
                <span className={`mcp-ref-score ${scoreClass}`}>{pct}% match</span>
              </div>
              <div className="mcp-ref-meta">Chunk #{ref.chunkIndex}</div>
              {ref.snippet && <div className="mcp-ref-snippet">{ref.snippet}</div>}
              {ref.sourceUrl && (
                <a
                  className="mcp-ref-link"
                  href={ref.sourceUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  View source &rarr;
                </a>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
