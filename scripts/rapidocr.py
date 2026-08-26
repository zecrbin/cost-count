import argparse
import json
import sys

from rapidocr_onnxruntime import RapidOCR


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description="One-shot batch OCR worker for Cost Count")
    parser.add_argument("--image", action="append", required=True)
    parser.add_argument("--use-cuda", choices=("true", "false"), default="true")
    args = parser.parse_args()

    ocr = RapidOCR(use_cuda=args.use_cuda == "true", print_verbose=False)
    results = []
    for image_path in args.image:
        try:
            result, _ = ocr(image_path)
            lines = [str(item[1]).strip() for item in result or [] if len(item) > 1 and str(item[1]).strip()]
            results.append({"text": "\n".join(lines), "error": ""})
        except Exception as exception:
            results.append({"text": "", "error": str(exception)})
    sys.stdout.write(json.dumps(results, ensure_ascii=False))


if __name__ == "__main__":
    main()
