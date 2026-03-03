import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { createGalleryServer } from "./server.js";

async function main() {
  const server = createGalleryServer();
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

main().catch((error) => {
  console.error("Gallery server failed to start:", error);
  process.exit(1);
});
