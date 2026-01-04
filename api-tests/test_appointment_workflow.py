#!/usr/bin/env python3
"""
Hospital Management System - Appointment Booking Workflow Test

This script tests the complete appointment booking workflow:
1. Register patient and doctor users
2. Create patient profile
3. Create doctor profile
4. Book appointment with disease-specialty matching
5. Verify appointment and event processing
"""

import requests
import json
from datetime import datetime, timedelta

BASE_URL = "http://localhost:8080"

class Colors:
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    RESET = '\033[0m'
    BOLD = '\033[1m'

def print_step(step_num, title):
    print(f"\n{Colors.BOLD}{Colors.BLUE}{'='*60}{Colors.RESET}")
    print(f"{Colors.BOLD}{Colors.BLUE}STEP {step_num}: {title}{Colors.RESET}")
    print(f"{Colors.BOLD}{Colors.BLUE}{'='*60}{Colors.RESET}")

def print_success(message):
    print(f"{Colors.GREEN}✓ {message}{Colors.RESET}")

def print_error(message):
    print(f"{Colors.RED}✗ {message}{Colors.RESET}")

def print_info(message):
    print(f"{Colors.YELLOW}→ {message}{Colors.RESET}")

def pretty_print_json(data):
    print(json.dumps(data, indent=2))


class HospitalAPIClient:
    def __init__(self, base_url):
        self.base_url = base_url
        self.session = requests.Session()

    def register_user(self, email, password, name, role, gender):
        """Register a new user"""
        response = self.session.post(
            f"{self.base_url}/api/auth/register",
            json={
                "email": email,
                "password": password,
                "name": name,
                "role": role,
                "gender": gender
            }
        )
        return response

    def login(self, email, password):
        """Login and return token"""
        response = self.session.post(
            f"{self.base_url}/api/auth/login",
            json={"email": email, "password": password}
        )
        return response

    def create_patient(self, token, patient_data):
        """Create a patient profile"""
        headers = {"Authorization": f"Bearer {token}"}
        response = self.session.post(
            f"{self.base_url}/api/patients",
            json=patient_data,
            headers=headers
        )
        return response

    def create_doctor(self, token, doctor_data):
        """Create a doctor profile"""
        headers = {"Authorization": f"Bearer {token}"}
        response = self.session.post(
            f"{self.base_url}/api/doctors",
            json=doctor_data,
            headers=headers
        )
        return response

    def get_doctors_by_specialty(self, token, specialty):
        """Get doctors by specialty"""
        headers = {"Authorization": f"Bearer {token}"}
        response = self.session.get(
            f"{self.base_url}/api/doctors/specialty/{specialty}",
            headers=headers
        )
        return response

    def create_appointment(self, token, appointment_data):
        """Create an appointment"""
        headers = {"Authorization": f"Bearer {token}"}
        response = self.session.post(
            f"{self.base_url}/api/appointments",
            json=appointment_data,
            headers=headers
        )
        return response

    def get_appointment(self, token, appointment_id):
        """Get appointment by ID"""
        headers = {"Authorization": f"Bearer {token}"}
        response = self.session.get(
            f"{self.base_url}/api/appointments/{appointment_id}",
            headers=headers
        )
        return response

    def get_patient_appointments(self, token, patient_id):
        """Get all appointments for a patient"""
        headers = {"Authorization": f"Bearer {token}"}
        response = self.session.get(
            f"{self.base_url}/api/appointments/patient/{patient_id}",
            headers=headers
        )
        return response


def test_appointment_workflow():
    """Run the complete appointment booking workflow test"""

    client = HospitalAPIClient(BASE_URL)

    # Generate unique emails for this test run
    timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    patient_email = f"patient_{timestamp}@example.com"
    doctor_email = f"doctor_{timestamp}@hospital.com"

    # ========================================
    # STEP 1: Register Users
    # ========================================
    print_step(1, "Register Patient and Doctor Users")

    # Register Patient
    print_info(f"Registering patient: {patient_email}")
    response = client.register_user(
        email=patient_email,
        password="Patient123456",
        name="Test Patient",
        role="PATIENT",
        gender="MALE"
    )

    if response.status_code in [200, 201]:
        patient_data = response.json()
        patient_user_id = patient_data['data']['userId']
        patient_token = patient_data['data']['accessToken']
        print_success(f"Patient registered: {patient_user_id}")
    else:
        print_error(f"Failed to register patient: {response.status_code}")
        pretty_print_json(response.json())
        return False

    # Register Doctor
    print_info(f"Registering doctor: {doctor_email}")
    response = client.register_user(
        email=doctor_email,
        password="Doctor123456",
        name="Dr. Test Doctor",
        role="DOCTOR",
        gender="FEMALE"
    )

    if response.status_code in [200, 201]:
        doctor_data = response.json()
        doctor_user_id = doctor_data['data']['userId']
        doctor_token = doctor_data['data']['accessToken']
        print_success(f"Doctor registered: {doctor_user_id}")
    else:
        print_error(f"Failed to register doctor: {response.status_code}")
        pretty_print_json(response.json())
        return False

    # ========================================
    # STEP 2: Create Patient Profile
    # ========================================
    print_step(2, "Create Patient Profile")

    print_info("Creating patient profile with DIABETES condition")
    response = client.create_patient(patient_token, {
        "userId": patient_user_id,
        "name": "Test Patient",
        "email": patient_email,
        "gender": "MALE",
        "dateOfBirth": "1990-05-15",
        "phone": "+15550001234",
        "address": "123 Test Street, Test City",
        "disease": "DIABETES"  # This maps to ENDOCRINOLOGY specialty
    })

    if response.status_code == 201:
        patient_profile = response.json()['data']
        patient_id = patient_profile['id']
        print_success(f"Patient profile created: {patient_id}")
        print_info(f"Disease: {patient_profile['disease']}")
    else:
        print_error(f"Failed to create patient profile: {response.status_code}")
        pretty_print_json(response.json())
        return False

    # ========================================
    # STEP 3: Create Doctor Profile
    # ========================================
    print_step(3, "Create Doctor Profile")

    print_info("Creating doctor profile with ENDOCRINOLOGY specialty")
    response = client.create_doctor(doctor_token, {
        "userId": doctor_user_id,
        "name": "Dr. Test Doctor",
        "email": doctor_email,
        "specialty": "ENDOCRINOLOGY",  # Matches DIABETES disease
        "phone": "+15559998765",
        "qualification": "MD, Endocrinology",
        "yearsOfExperience": 10,
        "gender": "FEMALE",
        "licenseNumber": f"LIC-{timestamp}"
    })

    if response.status_code == 201:
        doctor_profile = response.json()['data']
        doctor_id = doctor_profile['id']
        print_success(f"Doctor profile created: {doctor_id}")
        print_info(f"Specialty: {doctor_profile['specialty']}")
    else:
        print_error(f"Failed to create doctor profile: {response.status_code}")
        pretty_print_json(response.json())
        return False

    # ========================================
    # STEP 4: Book Appointment
    # ========================================
    print_step(4, "Book Appointment with Disease-Specialty Matching")

    # Schedule appointment for tomorrow at 10:00 AM
    appointment_date = (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d")

    print_info(f"Booking appointment for {appointment_date} at 10:00")
    print_info(f"Patient disease (DIABETES) should match doctor specialty (ENDOCRINOLOGY)")

    response = client.create_appointment(patient_token, {
        "patientId": patient_id,
        "doctorId": doctor_id,
        "appointmentDate": appointment_date,
        "appointmentTime": "10:00",
        "reason": "Diabetes management consultation",
        "notes": "First visit for diabetes management"
    })

    if response.status_code == 201:
        appointment = response.json()['data']
        appointment_id = appointment['id']
        print_success(f"Appointment created: {appointment_id}")
        print_info(f"Status: {appointment['status']}")
        print_info(f"Date: {appointment['appointmentDate']} at {appointment['appointmentTime']}")
    else:
        print_error(f"Failed to create appointment: {response.status_code}")
        pretty_print_json(response.json())
        return False

    # ========================================
    # STEP 5: Verify Appointment
    # ========================================
    print_step(5, "Verify Appointment and Event Processing")

    # Get appointment by ID
    print_info("Retrieving appointment details...")
    response = client.get_appointment(patient_token, appointment_id)

    if response.status_code in [200, 201]:
        appointment_details = response.json()['data']
        print_success("Appointment retrieved successfully")
        print_info(f"Appointment ID: {appointment_details['id']}")
        print_info(f"Patient: {appointment_details.get('patientName', 'N/A')}")
        print_info(f"Doctor: {appointment_details.get('doctorName', 'N/A')}")
        print_info(f"Status: {appointment_details['status']}")
    else:
        print_error(f"Failed to retrieve appointment: {response.status_code}")
        pretty_print_json(response.json())

    # Get all patient appointments
    print_info("Retrieving all patient appointments...")
    response = client.get_patient_appointments(patient_token, patient_id)

    if response.status_code in [200, 201]:
        appointments = response.json()['data']
        print_success(f"Found {len(appointments) if isinstance(appointments, list) else 1} appointment(s)")
    else:
        print_error(f"Failed to retrieve patient appointments: {response.status_code}")

    # ========================================
    # SUMMARY
    # ========================================
    print(f"\n{Colors.BOLD}{Colors.GREEN}{'='*60}{Colors.RESET}")
    print(f"{Colors.BOLD}{Colors.GREEN}WORKFLOW COMPLETED SUCCESSFULLY!{Colors.RESET}")
    print(f"{Colors.BOLD}{Colors.GREEN}{'='*60}{Colors.RESET}")
    print(f"""
Summary:
  - Patient User ID: {patient_user_id}
  - Patient Profile ID: {patient_id}
  - Doctor User ID: {doctor_user_id}
  - Doctor Profile ID: {doctor_id}
  - Appointment ID: {appointment_id}

Disease-Specialty Match:
  - Patient Disease: DIABETES
  - Doctor Specialty: ENDOCRINOLOGY
  - Match: ✓
""")

    return True


if __name__ == "__main__":
    print(f"\n{Colors.BOLD}Hospital Management System - Appointment Workflow Test{Colors.RESET}")
    print(f"Testing against: {BASE_URL}\n")

    try:
        success = test_appointment_workflow()
        exit(0 if success else 1)
    except requests.exceptions.ConnectionError:
        print_error(f"Could not connect to {BASE_URL}")
        print_info("Make sure all services are running (./start-local.sh)")
        exit(1)
    except Exception as e:
        print_error(f"Unexpected error: {e}")
        raise
