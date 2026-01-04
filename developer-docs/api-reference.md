# API Reference

All requests go through the API Gateway at `http://localhost:8080`.

## Authentication

### Register User

```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@hospital.com",
  "password": "SecurePass123",
  "name": "John Doe",
  "role": "PATIENT",
  "gender": "MALE"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@hospital.com",
    "name": "John Doe",
    "role": "PATIENT",
    "accessToken": "eyJhbGciOiJIUzUxMiJ9..."
  }
}
```

**Roles:** `ADMIN`, `DOCTOR`, `PATIENT`, `NURSE`, `RECEPTIONIST`

### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@hospital.com",
  "password": "SecurePass123"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@hospital.com",
    "name": "John Doe",
    "role": "PATIENT",
    "accessToken": "eyJhbGciOiJIUzUxMiJ9..."
  }
}
```

### Using JWT Token

Include the token in the `Authorization` header:

```http
GET /api/patients
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

## Patients

### Create Patient

```http
POST /api/patients
Authorization: Bearer {token}
Content-Type: application/json

{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Jane Patient",
  "email": "jane@hospital.com",
  "phone": "1234567890",
  "gender": "FEMALE",
  "dateOfBirth": "1990-05-15",
  "disease": "DIABETES",
  "bloodGroup": "O_POSITIVE",
  "address": "123 Main St",
  "emergencyContact": "John Doe",
  "emergencyPhone": "0987654321"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Patient created successfully",
  "data": {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Jane Patient",
    "email": "jane@hospital.com",
    "disease": "DIABETES",
    "active": true,
    "createdAt": "2026-01-04T12:00:00"
  }
}
```

**Diseases:** `DIABETES`, `HEART_DISEASE`, `HYPERTENSION`, `ASTHMA`, `CANCER`, `ARTHRITIS`, `MALARIA`, `DENGUE`, `TYPHOID`, `TUBERCULOSIS`, `PNEUMONIA`, `COVID_19`, `KIDNEY_DISEASE`, `LIVER_DISEASE`, `THYROID_DISORDER`, `MENTAL_HEALTH_DISORDER`, `NEUROLOGICAL_DISORDER`, `SKIN_DISEASE`, `EYE_DISEASE`, `ENT_DISORDER`, `BONE_FRACTURE`, `GASTROINTESTINAL_DISORDER`, `RESPIRATORY_DISORDER`, `OTHER`

### Get Patient by ID

```http
GET /api/patients/{id}
Authorization: Bearer {token}
```

### Get All Patients

```http
GET /api/patients
Authorization: Bearer {token}
```

### Update Patient

```http
PUT /api/patients/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Jane Updated",
  "phone": "9876543210",
  "address": "456 New St"
}
```

### Delete Patient

```http
DELETE /api/patients/{id}
Authorization: Bearer {token}
```

## Doctors

### Create Doctor

```http
POST /api/doctors
Authorization: Bearer {token}
Content-Type: application/json

{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Dr. Smith",
  "email": "drsmith@hospital.com",
  "phone": "1234567890",
  "gender": "MALE",
  "specialty": "CARDIOLOGY",
  "licenseNumber": "MED12345",
  "yearsOfExperience": 15,
  "qualifications": "MD, FACC",
  "consultationFee": 200.00
}
```

**Specialties:** `CARDIOLOGY`, `NEUROLOGY`, `ORTHOPEDICS`, `PEDIATRICS`, `DERMATOLOGY`, `OPHTHALMOLOGY`, `ENT`, `PSYCHIATRY`, `ONCOLOGY`, `GASTROENTEROLOGY`, `PULMONOLOGY`, `NEPHROLOGY`, `ENDOCRINOLOGY`, `RHEUMATOLOGY`, `GENERAL_MEDICINE`, `GENERAL_SURGERY`, `EMERGENCY_MEDICINE`, `INFECTIOUS_DISEASE`, `OTHER`

### Get Doctor by ID

```http
GET /api/doctors/{id}
Authorization: Bearer {token}
```

### Get All Doctors

```http
GET /api/doctors
Authorization: Bearer {token}
```

### Get Doctors by Specialty

```http
GET /api/doctors/specialty/{specialty}
Authorization: Bearer {token}
```

### Get Available Doctors

```http
GET /api/doctors/available
Authorization: Bearer {token}
```

## Appointments

### Create Appointment

```http
POST /api/appointments
Authorization: Bearer {token}
Content-Type: application/json

{
  "patientId": "660e8400-e29b-41d4-a716-446655440001",
  "doctorId": "770e8400-e29b-41d4-a716-446655440002",
  "appointmentDate": "2026-01-10T10:00:00",
  "reason": "Regular checkup",
  "notes": "Follow-up for diabetes management"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Appointment created successfully",
  "data": {
    "id": "880e8400-e29b-41d4-a716-446655440003",
    "patientId": "660e8400-e29b-41d4-a716-446655440001",
    "doctorId": "770e8400-e29b-41d4-a716-446655440002",
    "appointmentDate": "2026-01-10T10:00:00",
    "status": "PENDING"
  }
}
```

### Get Appointment

```http
GET /api/appointments/{id}
Authorization: Bearer {token}
```

### Get Patient Appointments

```http
GET /api/appointments/patient/{patientId}
Authorization: Bearer {token}
```

### Get Doctor Appointments

```http
GET /api/appointments/doctor/{doctorId}
Authorization: Bearer {token}
```

### Cancel Appointment

```http
PUT /api/appointments/{id}/cancel
Authorization: Bearer {token}
```

### Confirm Appointment

```http
PUT /api/appointments/{id}/confirm
Authorization: Bearer {token}
```

**Statuses:** `PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED`

## Medical Records

### Create Medical Record

```http
POST /api/medical-records
Authorization: Bearer {token}
Content-Type: application/json

{
  "patientId": "660e8400-e29b-41d4-a716-446655440001",
  "doctorId": "770e8400-e29b-41d4-a716-446655440002",
  "recordDate": "2026-01-04",
  "diagnosis": "Type 2 Diabetes Mellitus",
  "symptoms": "Increased thirst, frequent urination",
  "treatment": "Metformin 500mg twice daily",
  "notes": "Patient advised on diet modifications"
}
```

### Get Patient Medical Records

```http
GET /api/medical-records/patient/{patientId}
Authorization: Bearer {token}
```

### Create Prescription

```http
POST /api/prescriptions
Authorization: Bearer {token}
Content-Type: application/json

{
  "patientId": "660e8400-e29b-41d4-a716-446655440001",
  "doctorId": "770e8400-e29b-41d4-a716-446655440002",
  "medicalRecordId": "990e8400-e29b-41d4-a716-446655440004",
  "prescriptionDate": "2026-01-04",
  "medicationName": "Metformin",
  "dosage": "500mg",
  "frequency": "Twice daily",
  "duration": "30 days",
  "instructions": "Take with meals",
  "refillable": true,
  "refillsRemaining": 3
}
```

### Create Medical Report

```http
POST /api/medical-reports
Authorization: Bearer {token}
Content-Type: application/json

{
  "patientId": "660e8400-e29b-41d4-a716-446655440001",
  "doctorId": "770e8400-e29b-41d4-a716-446655440002",
  "reportType": "LAB",
  "reportDate": "2026-01-04",
  "reportTitle": "Blood Sugar Test",
  "reportContent": "Fasting glucose: 126 mg/dL, HbA1c: 7.2%"
}
```

**Report Types:** `LAB`, `XRAY`, `MRI`, `CT`, `ULTRASOUND`, `ECG`, `PATHOLOGY`, `OTHER`

## Facility (Rooms)

### Create Room

```http
POST /api/rooms
Authorization: Bearer {token}
Content-Type: application/json

{
  "roomNumber": "101-A",
  "roomType": "PRIVATE",
  "capacity": 1,
  "dailyRate": 500.00,
  "floor": "1",
  "wing": "A",
  "description": "Private room with AC and attached bathroom"
}
```

**Room Types:** `GENERAL`, `PRIVATE`, `ICU`, `EMERGENCY`, `OPERATION_THEATRE`, `RECOVERY`

### Get All Rooms

```http
GET /api/rooms
Authorization: Bearer {token}
```

### Get Available Rooms

```http
GET /api/rooms/available
Authorization: Bearer {token}
```

### Create Room Booking

```http
POST /api/room-bookings
Authorization: Bearer {token}
Content-Type: application/json

{
  "roomId": "aa0e8400-e29b-41d4-a716-446655440005",
  "patientId": "660e8400-e29b-41d4-a716-446655440001",
  "admissionDate": "2026-01-05",
  "expectedDischargeDate": "2026-01-10",
  "reason": "Post-surgery recovery"
}
```

## Audit Logs

### Get Audit Logs

```http
GET /api/audit/logs
Authorization: Bearer {token}
```

### Get Audit Logs by User

```http
GET /api/audit/logs/user/{userId}
Authorization: Bearer {token}
```

### Get Audit Logs by Entity

```http
GET /api/audit/logs/entity/{entityType}/{entityId}
Authorization: Bearer {token}
```

## Response Format

### Success Response

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2026-01-04T12:00:00"
}
```

### Error Response

```json
{
  "success": false,
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid request parameters",
  "path": "/api/patients",
  "validationErrors": [
    {
      "field": "email",
      "message": "Email is required"
    }
  ],
  "timestamp": "2026-01-04T12:00:00"
}
```

### HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK - Request successful |
| 201 | Created - Resource created |
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Missing/invalid token |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource not found |
| 409 | Conflict - Duplicate resource |
| 500 | Internal Server Error |

## Health Checks

### API Gateway Health

```http
GET /actuator/health
```

### Service Health (Direct Access)

```http
GET http://localhost:8081/actuator/health  # Auth
GET http://localhost:8082/actuator/health  # Patient
GET http://localhost:8083/actuator/health  # Doctor
GET http://localhost:8084/actuator/health  # Appointment
GET http://localhost:8085/actuator/health  # Medical Records
GET http://localhost:8086/actuator/health  # Facility
GET http://localhost:8087/actuator/health  # Notification
GET http://localhost:8088/actuator/health  # Audit
```

## Testing with cURL

```bash
# Set token variable after login
TOKEN="eyJhbGciOiJIUzUxMiJ9..."

# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@hospital.com","password":"Test123","name":"Test","role":"ADMIN","gender":"MALE"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@hospital.com","password":"Test123"}'

# Create patient
curl -X POST http://localhost:8080/api/patients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"userId":"...", "name":"Patient", "email":"patient@test.com", "gender":"MALE", "disease":"DIABETES"}'

# List patients
curl http://localhost:8080/api/patients \
  -H "Authorization: Bearer $TOKEN"
```
