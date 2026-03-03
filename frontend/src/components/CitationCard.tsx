import type { Citation } from '../types';

interface Props {
  citation: Citation;
}

export function CitationCard({ citation }: Props) {
  return (
    <div className="citation-card">
      <div className="citation-header">
        <span className="citation-file">{citation.sourceFile}</span>
        <span className="citation-score">
          {(citation.score * 100).toFixed(0)}% match
        </span>
      </div>
      <div className="citation-chunk">Chunk #{citation.chunkIndex}</div>
      <div className="citation-snippet">{citation.snippet}</div>
    </div>
  );
}
