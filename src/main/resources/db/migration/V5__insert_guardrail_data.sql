-- Insert initial guardrail data

-- Insert Guardrail entities
INSERT INTO guardrail (id, name, description, scope, external_references, metadata_keys) VALUES
(1, 'Guardrails.ai', 'Python SDK with input/output validation: toxicity, bias, hallucination, format/schema enforcement', 'BOTH', 
 '{"https://github.com/guardrails-ai/guardrails"}', 
 '{"sources", "embed_function", "pii_entities"}'),

(2, 'NeMo Guardrails', 'Programmable DSL-based SDK: input, output, RAG fact-checking, bias/equality flows', 'BOTH', 
 '{"https://docs.nvidia.com/nemo/guardrails/latest/index.html"}', 
 '{"knowledge_base", "rag_documents"}'),

(3, 'Llama Guard 3‑8B', 'Advanced safety classifier (8 B params) for both input/output moderation, multilingual (8 languages), tool-use aware (search, code), with high F1 (0.939) and low FPR (0.040)', 'BOTH', 
 '{"https://huggingface.co/meta-llama/Llama-Guard-3-8B"}', 
 '{"model_variant", "quantized", "languages_supported"}'),

(4, 'Detoxify', 'Transformer‑based output toxicity detection: insults, identity hate, severe toxicity', 'OUTPUT', 
 '{"https://github.com/unitaryai/detoxify"}', 
 '{}');

-- Insert Guardrail Metrics relationships
INSERT INTO guardrail_metrics (guardrail_id, task_metric_id) VALUES
-- Guardrails.ai metrics
(1, 4),  -- amb_bias_score_Age
(1, 5),  -- disamb_bias_score_Age
(1, 6),  -- amb_bias_score_Gender_identity
(1, 7),  -- disamb_bias_score_Gender_identity
(1, 8),  -- amb_bias_score_Race_ethnicity
(1, 9),  -- disamb_bias_score_Race_ethnicity
(1, 10), -- pct_stereotype
(1, 11), -- acc (truthfulqa_mc1)
(1, 12), -- acc_norm (truthfulqa_mc1)
(1, 13), -- acc (toxigen)
(1, 14), -- acc_norm (toxigen)
(1, 15), -- acc (ethics_cm)
(1, 16), -- acc_norm (ethics_cm)
(1, 17), -- acc (winogender)
(1, 18), -- acc_norm (winogender)

-- NeMo Guardrails metrics
(2, 4),  -- amb_bias_score_Age
(2, 5),  -- disamb_bias_score_Age
(2, 6),  -- amb_bias_score_Gender_identity
(2, 7),  -- disamb_bias_score_Gender_identity
(2, 8),  -- amb_bias_score_Race_ethnicity
(2, 9),  -- disamb_bias_score_Race_ethnicity
(2, 10), -- pct_stereotype
(2, 11), -- acc (truthfulqa_mc1)
(2, 12), -- acc_norm (truthfulqa_mc1)
(2, 15), -- acc (ethics_cm)
(2, 16), -- acc_norm (ethics_cm)
(2, 17), -- acc (winogender)
(2, 18), -- acc_norm (winogender)

-- Llama Guard metrics
(3, 4),  -- amb_bias_score_Age
(3, 5),  -- disamb_bias_score_Age
(3, 6),  -- amb_bias_score_Gender_identity
(3, 7),  -- disamb_bias_score_Gender_identity
(3, 8),  -- amb_bias_score_Race_ethnicity
(3, 9),  -- disamb_bias_score_Race_ethnicity
(3, 10), -- pct_stereotype
(3, 11), -- acc (truthfulqa_mc1)
(3, 12), -- acc_norm (truthfulqa_mc1)
(3, 13), -- acc (toxigen)
(3, 14), -- acc_norm (toxigen)
(3, 15), -- acc (ethics_cm)
(3, 16), -- acc_norm (ethics_cm)
(3, 17), -- acc (winogender)
(3, 18), -- acc_norm (winogender)

-- Detoxify metrics
(4, 13), -- acc (toxigen)
(4, 14); -- acc_norm (toxigen)

-- Update sequence values to prevent conflicts with existing data
SELECT setval('guardrail_SEQ', (SELECT MAX(id) FROM guardrail) + 1); 
