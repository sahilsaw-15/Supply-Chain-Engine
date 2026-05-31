# Software-Driven Supply Chain Monitoring with Industry 4.0 Integration

A scalable platform for **real-time supply chain monitoring and predictive analytics**.  
This system detects delivery risks, predicts delays, and enables data-driven decisions using **Industry 4.0 technologies**.

---

## Overview
This project combines **data processing, machine learning, and IoT integration** to monitor supply chain operations.  
It analyzes historical and simulated real-time data to predict late deliveries and generate **structured, actionable alerts** for risky shipments.

---

## Features
- Data ingestion (CSV)  
- Data preprocessing (cleaning, transformation, encoding)  
- Feature engineering (delay, profit ratio, shipping efficiency)  
- Predictive modeling for late delivery detection  
- Advanced alert system (operational, predictive, IoT, system-level)  
- Dashboard with KPIs, logs, and visualizations  
- Real-time shipment monitoring (simulated / IoT-ready)  

---

## Machine Learning Models
- Logistic Regression  
- Random Forest  
- XGBoost  

---

## System Workflow
1. Data Collection (CSV / IoT-ready)  
2. Data Preprocessing  
3. Feature Engineering  
4. Model Prediction  
5. Alert Generation  
6. Dashboard Visualization  

---

## Tech Stack

### Backend
- Python (Pandas, NumPy, Scikit-learn)  
- Flask / FastAPI  
- SQLAlchemy  
- PostgreSQL / MySQL  

### IoT & Industry 4.0 Integration
- MQTT Protocol  
- Node-RED / EdgeX  
- InfluxDB (Time-Series DB)  

---

## Alert System

### A. Operational Alerts
- **ETA Deviation Alert** → triggers when predicted delivery time exceeds acceptable threshold  
- **Shipment Stoppage / Idle Alert** → detects vehicle inactivity beyond defined time window  

### B. Predictive Alerts
- **Predictive Maintenance Alert** → predicts potential vehicle/machine failure  
- **Delay Risk Spike Alert** → flags sudden increase in delay probability  
- **Confidence-Based Alert** → triggers only when model confidence exceeds threshold  

### C. IoT / Sensor Alerts
- **Temperature / Humidity Breach** → detects unsafe environmental conditions  
- **Shock / Vibration Alert** → identifies potential product damage  
- **Geo-fence Violation Alert** → detects route deviation or unauthorized movement  
- **Fuel / Battery Alert** → monitors low fuel or energy levels  
- **Device Offline Alert** → detects sensor/gateway disconnection  

### D. System / Pipeline Alerts
- **Data Ingestion Failure Alert** → identifies missing or failed data streams  
- **Pipeline Latency Alert** → detects processing delays beyond SLA  
- **Anomaly Detection Alert** → detects unusual patterns using ML  

### E. Notification & Response
- Mobile Push Notifications  
- Dashboard Real-time Popups  
- Alert Escalation Logic → escalates unresolved alerts to higher authority  

---

## Dashboard Insights
- Late delivery risk analysis  
- Shipping performance metrics  

---

## Workflow Pipeline
Order Placed → Data Collected → Processed → Model Prediction → Alert Generated → Dashboard Updated → Action Taken  

---

## Benefits
- Reduce late deliveries  
- Enable real-time monitoring  
- Optimize logistics & routing  
- Improve customer satisfaction  
- Support data-driven decision making  

---

## Future Work
- Real-time IoT streaming integration  
- Digital Twin of supply chain  
- Reinforcement learning for route optimization  
- Blockchain for secure logistics transactions  

---

## Evaluation Metrics
- Accuracy  
- Precision  
- Recall  
- F1-Score  
- ROC-AUC  
- Confusion Matrix  

---

## Authors
- Elsayed, Abdelrahman  
- Covaria, Sergio  
- Montalvan, Daniel  
- Bhogal, Parminder Singh  
- Sawant, Sahil  


<img width="1024" height="1536" alt="sketch3" src="https://github.com/user-attachments/assets/c412591b-b98d-4baf-8def-28db20a30deb" />
