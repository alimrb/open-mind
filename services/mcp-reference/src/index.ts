import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { createReferenceServer } from "./server.js";

async function main() {
  const server = createReferenceServer();
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

main().catch((error) => {
  console.error("Reference server failed to start:", error);
  process.exit(1);
});
