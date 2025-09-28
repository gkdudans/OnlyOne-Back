# OnlyOne-Back
<img width="931" height="504" alt="image" src="https://github.com/user-attachments/assets/576025f7-ba07-47e6-8307-08a8d9794ecc" />

## 📌 프로젝트 개요
> **액티브 시니어란?**  
> 은퇴 이후에도 하고 싶은 일을 능동적으로 찾는 50~60대를 일컫는 말로,  
> 적극적으로 소비하고 문화 활동에 나선다는 점에서 ‘실버 세대'와 구분되는 시니어
> 
액티브 시니어는 관심과 의지는 충분하지만, 모임을 찾고 오프라인으로 전환하는 과정에서 주저하게 됩니다.  
벗킷(Bukkit)은 그런 시니어들을 위해 만들어진 “손쉽게 모임을 찾고 참여할 수 있는 시니어 콘텐츠 플랫폼”입니다.

- **프로젝트 기간**: 2025.07 ~ 2025.09
- **프로젝트 참여 인원**: 6 (풀스택 개발)
- **시연 영상** https://drive.google.com/file/d/1P80JXV3FX1BPlqDrWWR8dHxgiOI-zlSb/view
- **담당 역할**: 정산(Settlement)·결제(Payment) 도메인 풀스택 개발
<br></br>

## 🔨 기술 스택 - 정산·결제 도메인 
<div align="center">
  <img src="https://img.shields.io/badge/Java-007396?style=flat-square&logo=Java&logoColor=white" style="height: 26px; margin: 3px;">
  <img src="https://img.shields.io/badge/Spring-6DB33F?style=flat-square&logo=Spring&logoColor=white" style="height: 26px; margin: 3px;">
  <img src="https://img.shields.io/badge/SpringBoot-6DB33F?style=flat-square&logo=Spring&logoColor=white" style="height: 26px; margin: 3px;">
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=MySQL&logoColor=white" style="height: 26px; margin: 3px;">
  <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=Docker&logoColor=white" style="height: 26px; margin: 3px;">
  <img src="https://img.shields.io/badge/AWS-232F3E?style=flat-square&logo=Amazon%20AWS&logoColor=white" style="height: 26px; margin: 3px;">
</div>

<div align="center">
  <img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=Redis&logoColor=white" style="height: 26px; margin: 3px;">
  <img src="https://img.shields.io/badge/Kafka-231F20?style=flat-square&logo=Apache%20Kafka&logoColor=white" style="height: 26px; margin: 3px;">
  <img src="https://img.shields.io/badge/Jacoco-4B32C3?style=flat-square&logoColor=white" style="height: 26px; margin: 3px;">
  <img src="https://img.shields.io/badge/JUnit-25A162?style=flat-square&logo=JUnit5&logoColor=white" style="height: 26px; margin: 3px;">
  <img src="https://img.shields.io/badge/nGrinder-000000?style=flat-square&logoColor=white" style="height: 26px; margin: 3px;">
  <img src="https://img.shields.io/badge/Prometheus-E6522C?style=flat-square&logo=Prometheus&logoColor=white" style="height: 26px; margin: 3px;">
  <img src="https://img.shields.io/badge/Grafana-F46800?style=flat-square&logo=Grafana&logoColor=white" style="height: 26px; margin: 3px;">
</div>

<br></br>

## 💟 서비스 아키텍처
<img width="1338" height="686" alt="image (5)" src="https://github.com/user-attachments/assets/15721bda-fdb6-4e59-a4fe-7cbb8456a814" />

## ⚡담당 핵심 기능 (풀스택 개발)

### 1️⃣ 충전 
- 다양한 결제 수단 지원: 카드, 계좌이체, 간편결제(토스·카카오페이 등)를 통해 손쉽게 충전 가능합니다.
- 실시간 충전 반영: 충전이 완료되면 즉시 포인트 잔액에 반영되어 곧바로 사용 가능합니다.
- 거래 내역 조회: 충전/정산 내역을 분리해 마이페이지에서 쉽게 확인할 수 있습니다.

### 2️⃣ 정산 & 정기 모임
- 자동 정산: 정기 모임 참여자 대상으로 리더가 자동 정산을 요청할 수 있으며, 모임 참여 시 설정된 참여비가 예약금으로 홀드되고 정산 시 차감됩니다.
- 정기 모임 관리: 모임 내에서 오프라인 정기 모임(정모)을 생성하고 관리할 수 있습니다.
- 정산 로그 관리: 정산 성공/실패 이력을 이벤트 로그 형태로 데이터베이스에 저장하여 관리합니다.

