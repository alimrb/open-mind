import type { AgentStep } from '../types';

interface Props {
  steps: AgentStep[];
  defaultOpen?: boolean;
}

export function AgentSteps({ steps, defaultOpen = false }: Props) {
  if (steps.length === 0) return null;

  // Group consecutive tool_call/thinking steps into collapsible blocks
  const toolSteps = steps.filter((s) => s.type === 'tool_call');
  const thinkingSteps = steps.filter((s) => s.type === 'thinking');

  if (toolSteps.length === 0 && thinkingSteps.length === 0) return null;

  return (
    <div className="agent-steps">
      {thinkingSteps.length > 0 && (
        <details className="agent-step-group thinking" open={defaultOpen || undefined}>
          <summary>
            <span className="agent-step-icon">&#9673;</span>
            <span className="agent-step-summary">
              Thinking ({thinkingSteps.length} step{thinkingSteps.length > 1 ? 's' : ''})
            </span>
          </summary>
          <div className="agent-step-details">
            {thinkingSteps.map((step, i) => (
              <div key={`think-${i}`} className="agent-step-item">
                {step.content || 'Processing...'}
              </div>
            ))}
          </div>
        </details>
      )}

      {toolSteps.length > 0 && (
        <details className="agent-step-group tool-call" open={defaultOpen || undefined}>
          <summary>
            <span className="agent-step-icon">&#9881;</span>
            <span className="agent-step-summary">
              Tool calls ({toolSteps.length})
            </span>
          </summary>
          <div className="agent-step-details">
            {toolSteps.map((step, i) => (
              <div key={`tool-${i}`} className="agent-step-item">
                <span className="agent-step-tool-name">{step.title}</span>
                {step.content && (
                  <span className="agent-step-tool-detail">{step.content}</span>
                )}
              </div>
            ))}
          </div>
        </details>
      )}
    </div>
  );
}
