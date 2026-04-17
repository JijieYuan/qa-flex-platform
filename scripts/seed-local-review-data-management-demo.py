import json
import urllib.request

BASE_URL = "http://localhost:18080"


def request_json(method: str, path: str, payload=None):
    url = f"{BASE_URL}{path}"
    data = None
    headers = {"Content-Type": "application/json; charset=utf-8"}
    if payload is not None:
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(request) as response:
        return json.loads(response.read().decode("utf-8"))


def main():
    existing = request_json("GET", "/api/review-data/records?page=1&size=200")["data"]["records"]
    for record in existing:
        print(f"Deleting review record #{record['id']} - {record['title']}")
        request_json("DELETE", f"/api/review-data/records/{record['id']}")

    seed_records = [
        {
            "projectName": "CC2026R3",
            "title": "[草图模块] 算数功能设计说明书评审",
            "moduleName": "草图",
            "reviewType": "设计说明书评审",
            "reviewDate": "2026-02-10",
            "reviewOwner": "王强",
            "reviewExperts": ["张晓涵", "王强", "杨亚伦"],
            "reviewScalePages": 24,
            "reviewProduct": "算数功能设计说明书",
            "authorName": "路士坤",
            "reviewVersion": "V1.3",
            "problemItems": [
                {
                    "reviewerName": "张晓涵",
                    "workloadHours": 1.0,
                    "reviewCategory": "会议评审",
                    "documentPosition": "3.3.2",
                    "problemCategory": "文档规范",
                    "problemDescription": "样条曲线重新插点的流程图描述不完整，关键分支缺少输入输出说明。",
                    "suggestedSolution": "补充流程图中的关键节点说明，并统一箭头方向和命名方式。",
                    "ownerName": "路士坤",
                    "rejectionReason": "",
                    "problemStatus": "已修复",
                },
                {
                    "reviewerName": "王强",
                    "workloadHours": 1.0,
                    "reviewCategory": "会议评审",
                    "documentPosition": "3.3.2",
                    "problemCategory": "功能性",
                    "problemDescription": "单段线从非参数线变为参考线后，尺寸约束联动范围描述不明确。",
                    "suggestedSolution": "补充“尺寸保留/清除”的边界说明，并增加一段典型例子。",
                    "ownerName": "路士坤",
                    "rejectionReason": "",
                    "problemStatus": "已修复",
                },
            ],
        },
        {
            "projectName": "CC2026R3",
            "title": "[草图模块] 样条曲线-曲率控制功能设计评审",
            "moduleName": "草图",
            "reviewType": "设计说明书评审",
            "reviewDate": "2026-02-10",
            "reviewOwner": "王强",
            "reviewExperts": ["杨亚伦", "武伟"],
            "reviewScalePages": 33,
            "reviewProduct": "样条曲线功能设计说明书",
            "authorName": "马传超",
            "reviewVersion": "V2.0",
            "problemItems": [
                {
                    "reviewerName": "杨亚伦",
                    "workloadHours": 0.2,
                    "reviewCategory": "会议评审",
                    "documentPosition": "3.5 算法接口",
                    "problemCategory": "文档规范",
                    "problemDescription": "命名不规范，局部变量与接口描述中的名词不一致。",
                    "suggestedSolution": "统一接口名和文档描述中的术语，补充缩写说明。",
                    "ownerName": "马传超",
                    "rejectionReason": "",
                    "problemStatus": "已修复",
                },
                {
                    "reviewerName": "武伟",
                    "workloadHours": 0.2,
                    "reviewCategory": "会议评审",
                    "documentPosition": "4.2 约束设计",
                    "problemCategory": "完整性",
                    "problemDescription": "转述结构与原公式之间缺少映射关系，读者难以定位结论来源。",
                    "suggestedSolution": "在公式旁补充变量说明，并给出对应的推导简表。",
                    "ownerName": "马传超",
                    "rejectionReason": "",
                    "problemStatus": "新提出",
                },
            ],
        },
        {
            "projectName": "CC2026R3",
            "title": "[工具模块] 速度样式功能设计说明书评审",
            "moduleName": "工具",
            "reviewType": "设计说明书评审",
            "reviewDate": "2026-02-09",
            "reviewOwner": "李利",
            "reviewExperts": ["武伟", "崔雪峰"],
            "reviewScalePages": 38,
            "reviewProduct": "速度样式功能设计说明书",
            "authorName": "张磊",
            "reviewVersion": "V1.0",
            "problemItems": [
                {
                    "reviewerName": "崔雪峰",
                    "workloadHours": 0.3,
                    "reviewCategory": "会议评审",
                    "documentPosition": "1.5 动画问题",
                    "problemCategory": "完整性",
                    "problemDescription": "加上控制阀后无法一步撤销，异常流处理没有说明。",
                    "suggestedSolution": "补充撤销链路说明，并明确异常流下的回退策略。",
                    "ownerName": "张磊",
                    "rejectionReason": "",
                    "problemStatus": "已修复",
                },
                {
                    "reviewerName": "武伟",
                    "workloadHours": 0.2,
                    "reviewCategory": "独立评审",
                    "documentPosition": "1.4 前后置任务",
                    "problemCategory": "功能性",
                    "problemDescription": "旋转显示元宽限制描述不够，边界条件缺少示意。",
                    "suggestedSolution": "增加边界条件表格，并补一张极值示意图。",
                    "ownerName": "张磊",
                    "rejectionReason": "已确认先按现有实现交付，后续版本再补充极值示意。",
                    "problemStatus": "已拒绝",
                },
            ],
        },
        {
            "projectName": "CC2026R3",
            "title": "[平台模块] 外部参考改进（服务端缓存）评审",
            "moduleName": "平台",
            "reviewType": "需求说明书评审",
            "reviewDate": "2026-02-09",
            "reviewOwner": "徐建民",
            "reviewExperts": ["徐建民"],
            "reviewScalePages": 19,
            "reviewProduct": "外部参考改进需求说明书",
            "authorName": "徐建民",
            "reviewVersion": "V0.9",
            "problemItems": [
                {
                    "reviewerName": "徐建民",
                    "workloadHours": 0.5,
                    "reviewCategory": "独立评审",
                    "documentPosition": "2.1 范围定义",
                    "problemCategory": "功能性",
                    "problemDescription": "缓存失效策略只描述了定时清理，没有覆盖主动刷新场景。",
                    "suggestedSolution": "补充主动刷新与失效重建流程，明确缓存穿透的处理策略。",
                    "ownerName": "徐建民",
                    "rejectionReason": "",
                    "problemStatus": "新提出",
                },
            ],
        },
    ]

    for record in seed_records:
        problem_items = record.pop("problemItems")
        print(f"Creating review record: {record['title']}")
        created = request_json("POST", "/api/review-data/records", record)
        record_id = created["data"]["record"]["id"]
        for problem in problem_items:
            request_json("POST", f"/api/review-data/records/{record_id}/problem-items", problem)

    print("Review data demo seed completed.")


if __name__ == "__main__":
    main()
