# ReflectAI

ReflectAI is a mobile journaling application that combines traditional journaling with artificial intelligence to provide users with emotional support and insights. Built with a minimalist design philosophy, the app offers a distraction-free environment where users can express their thoughts and receive supportive, therapist-like feedback.

## Screenshots

### Home Screen with Calendar View
<img width="349" alt="image" src="https://github.com/user-attachments/assets/6b9ab75e-8b4a-4742-b579-d83b2a2bac53" />


### Journal Entry Screen
<img width="349" alt="image" src="https://github.com/user-attachments/assets/3fcc4f2d-25ed-4ea9-ab2b-6d5739b7e01a" />


### AI Therapist Response
<img width="349" alt="image" src="https://github.com/user-attachments/assets/4e7f36b0-03de-42d3-ae2d-124242f8ffa0" />


## Features

- **Journal Entry Creation**: Write journal entries with mood tracking
- **AI-Generated Feedback**: Receive personalized therapeutic responses
- **Calendar Visualization**: Track mood patterns over time with color-coded calendar
- **Voice-to-Text**: Dictate journal entries using speech recognition
- **Responsive Design**: Optimized layouts for both phones and tablets
- **Secure Authentication**: Email/password authentication with Firebase

## Technical Architecture

ReflectAI follows the MVVM (Model-View-ViewModel) architecture pattern:

- **UI Layer**: Built with Jetpack Compose for a modern, declarative UI
- **ViewModel Layer**: Manages UI state and business logic
- **Repository Layer**: Handles data operations and external service integration
- **Model Layer**: Defines domain entities and relationships

### Key Components

- Custom calendar implementation with Java Time API
- Firebase Realtime Database for data storage
- Firebase Authentication for user management
- OpenAI API integration for sentiment analysis and therapeutic responses
- Android speech recognition for voice input

## Setup Instructions

1. Clone the repository
   ```
   git clone https://github.com/yourusername/ReflectAI.git
   ```

2. Open the project in Android Studio

3. Create a `local.properties` file with your API keys:
   ```
   OPENAI_API_KEY=your_openai_api_key
   ```

4. Connect to Firebase:
   - Create a Firebase project
   - Add your Android app to the project

5. Build and run the application

## Database Structure

```
firebase-database/
├── journal_entries/
│   └── {userId}/
│       ├── {entryId1}/  # Journal entry with content, mood, date, and AI analysis
│       ├── {entryId2}/
│       └── ...
└── users/
    └── {userId}/
        ├── id
        ├── displayName
        ├── email
        └── moodCounts/  # Aggregated mood statistics
```


## Technologies Used

- Kotlin
- Jetpack Compose
- Firebase (Authentication, Realtime Database)
- OpenAI API
- MVVM Architecture
- Coroutines and Flow
- Material 3 Design

## Author

Lucas Chen

