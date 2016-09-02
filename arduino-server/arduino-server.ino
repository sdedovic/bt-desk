#include <Servo.h>

Servo servo;
int pos = 0;
byte state = (byte) 255;

void setup() {
  servo.attach(9);
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
  }
  delay(50);
}

void moveUp() {
  if(pos < 180) {
    pos++;
    servo.write(pos);
  }
}

void moveDown() {
  if(pos > 0) {
    pos--;
    servo.write(pos);
  }
}

