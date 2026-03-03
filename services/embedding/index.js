import http from "node:http";
import { pipeline } from "@xenova/transformers";

const PORT = process.env.EMBEDDING_PORT || 4001;
const MODEL = "Xenova/all-MiniLM-L6-v2"; // 384-dim, fast, lightweight
const TARGET_DIM = 768; // Pad to match Qdrant collection config

let embedder = null;

async function getEmbedder() {
  if (!embedder) {
    console.log(`Loading embedding model: ${MODEL}`);
    embedder = await pipeline("feature-extraction", MODEL);
    console.log("Embedding model loaded");
  }
  return embedder;
}

// Pad 384-dim vector to 768-dim by repeating
function padVector(vec, targetDim) {
  if (vec.length >= targetDim) return vec.slice(0, targetDim);
  const padded = new Array(targetDim);
  for (let i = 0; i < targetDim; i++) {
    padded[i] = vec[i % vec.length];
  }
  return padded;
}

async function generateEmbedding(text) {
  const pipe = await getEmbedder();
  const output = await pipe(text, { pooling: "mean", normalize: true });
  const vec = Array.from(output.data);
  return padVector(vec, TARGET_DIM);
}

const server = http.createServer(async (req, res) => {
  if (req.method === "POST" && req.url === "/embed") {
    let body = "";
    for await (const chunk of req) body += chunk;

    try {
      const { text, texts } = JSON.parse(body);

      if (texts && Array.isArray(texts)) {
        const embeddings = [];
        for (const t of texts) {
          embeddings.push(await generateEmbedding(t));
        }
        res.writeHead(200, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ embeddings }));
      } else if (text) {
        const embedding = await generateEmbedding(text);
        res.writeHead(200, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ embedding }));
      } else {
        res.writeHead(400, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ error: "Provide 'text' or 'texts'" }));
      }
    } catch (err) {
      console.error("Embedding error:", err.message);
      res.writeHead(500, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ error: err.message }));
    }
  } else if (req.method === "GET" && req.url === "/health") {
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ status: "ok", model: MODEL }));
  } else {
    res.writeHead(404);
    res.end("Not found");
  }
});

// Pre-load model
getEmbedder().then(() => {
  server.listen(PORT, () => {
    console.log(`Embedding service running on http://localhost:${PORT}`);
  });
});
