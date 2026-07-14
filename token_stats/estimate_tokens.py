#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
基于字符规则的 token 近似估算器（可复现）。
说明：LLM 的精确 token 数只能由对应模型的 tokenizer 给出；
本脚本用字符规则做工程近似，供"规模感知"参考，非精确值。

规则（两套口径）：
- 国产模型口径（Qwen/GLM 等）：中文约 1 字 ≈ 1 token；
  英文/数字/标点/代码 约 4 字符 ≈ 1 token；空白同算。
- GPT 系口径（cl100k 等）：中文约 1.6 字符 ≈ 1 token；
  英文/符号约 4 字符 ≈ 1 token。
"""
import sys
import re

CJK_RE = re.compile(r'[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]')


def estimate(text: str):
    cjk = len(CJK_RE.findall(text))
    other = len(CJK_RE.sub('', text))  # 非 CJK 字符（含英文/数字/标点/代码）
    ws = text.count(' ') + text.count('\n') + text.count('\t') + text.count('\r')
    # 其他非空白字符（去掉空白）
    other_nows = max(0, other - ws)
    domestic = cjk + (other_nows + ws) / 4.0          # 国产模型口径
    gpt = cjk / 1.6 + (other_nows + ws) / 4.0          # GPT 系口径
    return {
        'chars': len(text),
        'cjk': cjk,
        'other_nows': other_nows,
        'ws': ws,
        'domestic': domestic,
        'gpt': gpt,
    }


def main():
    if len(sys.argv) < 2:
        print("用法: python3 estimate_tokens.py <text-file>")
        sys.exit(1)
    with open(sys.argv[1], encoding='utf-8') as f:
        text = f.read()
    r = estimate(text)
    print(f"文件: {sys.argv[1]}")
    print(f"字符总数        : {r['chars']}")
    print(f"  CJK 汉字       : {r['cjk']}")
    print(f"  其他非空白字符 : {r['other_nows']}")
    print(f"  空白字符       : {r['ws']}")
    print("-" * 40)
    print(f"估算 token（国产模型口径, 中文1字≈1tok）: {r['domestic']:.0f}")
    print(f"估算 token（GPT 系口径,   中文1.6字≈1tok）: {r['gpt']:.0f}")
    print(f"区间参考: {r['gpt']:.0f} ~ {r['domestic']:.0f} token")


if __name__ == '__main__':
    main()
