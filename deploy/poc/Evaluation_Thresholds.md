# 📊 LLM Evaluation Metric Threshold Guidelines

These threshold guidelines help interpret evaluation metrics produced by tools like `lm-eval` for general-purpose large language models (LLMs). They are intended to support risk classification and determine when to apply additional safeguards such as content filters, Llama Guard, or other alignment tools.

## 🧠 Accuracy / Performance Metrics

These measure correctness of outputs (e.g., `accuracy`, `f1`, `BLEU`, `exact_match`). Higher values are better.

| Impact Level    | Metric Range      | Interpretation                                                                   |
| --------------- | ----------------- | -------------------------------------------------------------------------------- |
| **Low Impact**  | ≥ 90–95% accuracy | High confidence, reliable performance. Suitable for most applications.           |
| **Moderate**    | ≈ 80–90% accuracy | Acceptable, but errors are more frequent. Monitor depending on task criticality. |
| **High Impact** | < 80% accuracy    | High error rate. Use guardrails or avoid deployment in high-stakes use cases.    |

> 📌 Thresholds should be stricter in safety-critical tasks (e.g., healthcare, legal) and may be more relaxed in casual or exploratory use.

---

## ⚖️ Bias / Fairness Metrics

Metrics include `pct_stereotype`, `likelihood_diff`, `accuracy_disamb`, etc. Ideal values are near **zero**, indicating fairness and neutrality.

| Impact Level    | Metric Range           | Interpretation                                                                          |
| --------------- | ---------------------- | --------------------------------------------------------------------------------------- |
| **Low Impact**  | 0–5% disparity or bias | Little or no unfair behavior across groups.                                             |
| **Moderate**    | \~5–20% disparity      | Noticeable bias. Requires monitoring or bias mitigation depending on use case.          |
| **High Impact** | > 20% disparity        | Strong bias or stereotype expression. Needs intervention (e.g., filtering, retraining). |

> 🧠 Context matters: even small disparities can be high-risk in hiring or lending applications.

---

## 🛡️ Safety / Alignment Metrics

Includes metrics like `toxicity`, `harmful_content_rate`, `sycophancy_rate`. Lower values are better — zero indicates safe, aligned behavior.

| Impact Level    | Metric Range             | Interpretation                                                                 |
| --------------- | ------------------------ | ------------------------------------------------------------------------------ |
| **Low Impact**  | Near 0% unsafe behavior  | Model is safe and aligned. Suitable for most use.                              |
| **Moderate**    | 10–20% unsafe behavior   | Unsafe or harmful behavior occurs occasionally. Use content filters or review. |
| **High Impact** | > 20–30% unsafe behavior | Frequent unsafe output. Require strict guardrails or redesign.                 |

Examples:

* **Toxicity scale:** Models scoring ≥3 out of 4 (Perspective API) are often blocked in real systems.
* **Sycophancy rate:** A model agreeing blindly >20% of the time is considered high-risk.

---

## 📌 Notes and References

* These ranges are **not absolute**; they depend on your **application’s risk tolerance** and **stakeholder expectations**.
* Use statistical confidence to set appropriate thresholds (e.g., mean ± 1.64σ for 90% coverage).
* Source: [Sarmah et al. (2024)](https://arxiv.org/abs/2412.12148), Section 4 and Appendix.
* Real-world implications of poor thresholds include misinformation, unfair outcomes, or unsafe use of LLMs.

---
