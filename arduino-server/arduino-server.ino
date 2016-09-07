#include <Servo.h>

int upPin = 4;
int downPin = 3;

int pos = 0;
byte state = (byte) 255;

void setup() {
  pinMode(upPin, OUTPUT);
  pinMode(downPin, OUTPUT);

  digitalWrite(downPin, LOW);
  digitalWrite(upPin, LOW);
  
  Serial.begin(9600);
}

void loop() {
  if (Serial.available() > 0) {
    byte in = Serial.read();
    state = in;
  }

  if(state == (byte) 1) {
    moveUp();
  } else if(state == (byte) 2) {
    moveDown();
  } else if(state == (byte) 255) {
    stopM();
  }
  delay(50);
}

void moveUp() {
  digitalWrite(downPin, LOW);
  digitalWrite(upPin, HIGH);
}

void moveDown() {
  digitalWrite(upPin, LOW);
  digitalWrite(downPin, HIGH);
}

void stopM() {
   digitalWrite(downPin, LOW);
  digitalWrite(upPin, LOW);
}

