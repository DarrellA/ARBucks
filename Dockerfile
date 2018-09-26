FROM gcr.io/cloud-builders/javac

# Install Dependencies
RUN apt-get update \
    && apt-get install -y wget zip unzip \
    && mkdir -p /opt/android-sdk-linux

ENV ANDROID_HOME /opt/android-sdk-linux

# Download Android SDK tools
RUN wget -q "https://dl.google.com/android/repository/sdk-tools-linux-4333796.zip" -O sdk-tools.zip \
    && unzip -q -d $ANDROID_HOME sdk-tools.zip \
    && rm sdk-tools.zip

ENV PATH $PATH:$ANDROID_HOME/tools/bin

# Install Android SDK components
RUN echo y | sdkmanager --install 'platforms;android-27' "build-tools;27.0.3" "platform-tools"
