import { useState } from 'react';
import type { GalleryImage } from '../types';

interface Props {
  data: string;
}

export function McpGallery({ data }: Props) {
  const [failedUrls, setFailedUrls] = useState<Set<string>>(new Set());

  let images: GalleryImage[] = [];
  try {
    const parsed = JSON.parse(data);
    images = parsed.images || [];
  } catch {
    return null;
  }

  if (images.length === 0) return null;

  const visibleImages = images.filter((img) => !failedUrls.has(img.url));
  if (visibleImages.length === 0) return null;

  return (
    <div className="mcp-gallery">
      <div className="mcp-gallery-header">
        <span className="mcp-gallery-title">Related Images</span>
        <span className="mcp-gallery-count">{visibleImages.length}</span>
      </div>
      <div className="mcp-gallery-grid">
        {visibleImages.map((img, i) => (
          <div key={`${img.url}-${i}`} className="mcp-gallery-item">
            <img
              src={img.url}
              alt={img.alt || ''}
              loading="lazy"
              onError={() => setFailedUrls((prev) => new Set(prev).add(img.url))}
            />
            {img.caption && <div className="mcp-gallery-caption">{img.caption}</div>}
            {img.source && <div className="mcp-gallery-source">Source: {img.source}</div>}
          </div>
        ))}
      </div>
    </div>
  );
}
