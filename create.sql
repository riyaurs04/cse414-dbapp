CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Appointments (
    Time date,
    appointmentID varchar(255),
    caregiverUsername varchar(255) REFERENCES Caregivers(Username),
    patientUsername varchar(255) REFERENCES Patients(Username),
    vaccineName varchar(255) REFERENCES Vaccines(Name),
    PRIMARY KEY (appointmentID)
);

CREATE TABLE Patients (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);
