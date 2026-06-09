## Phase 0: 프로젝트 실행
- 코드 분석을 통해 .claude/rules의 문서의 빈 부분들은 완성한다.
- Application이 run할 수 있도록 환경을 체크한다.
- Application 작동이 안 될 때 (에러가 날 때) 그게 정산 기능과 관련없는 코드의 문제라면 관련 코드 주석 처리
- 정산 기능과 관련있는 부분이라면 문제 및 해결 방법을 제시한다.


## Phase 1: 현재 구현 코드에 대한 이해
- target:
    - controller: summary = "정산 요청 생성" API
    - service: automaticSettlement (package com.example.onlyone.domain.settlement.service;)
    - OutboxAppender: Kafka 정산 시작 이벤트 발행
    - LedgerWriter: 정산 이벤트를 읽고 정산 수행 및 로그 기록

    - 해당 기능의 흐름을 파악한 뒤 정리해서 나한테 보고.

## Phase 2: 코드의 문제점, 개선 방향 파악
- 현재 코드는 동기 처리 -> 비동기+가상스레드 -> Redis Lua Script -> kafka 순서로 발전.
- 현재 코드 품질 평가. 단계별 평가도 ok.
- 나의 고민은 Kafka 적용 전으로 돌아가면서 재시도/실패 처리를 더 확실히 할지,
  Kafka를 적용하면서 안정성을 더 확보할지 고민.
- 특히, Kafka를 사용하면서 정산 실패에 대한 재시도/실패 처리가 불분명. 이에 대한 피드백.

## Phase 3: 코드 리팩토링
- 리팩토링 계획을 세운 뒤 수행.

## Phase 4: '결제' 기능에 대한 리팩토링 수행. 