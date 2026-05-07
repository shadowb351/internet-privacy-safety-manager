import google.generativeai as genai
import traceback

try:
    genai.configure(api_key='AIzaSyCGq7WjlqgCReBcMmLqa1vQkb7fH7DHzZA')
    m = genai.GenerativeModel('gemini-2.5-flash')
    response = m.generate_content('hello')
    print("SUCCESS")
    print(response.text)
except Exception as e:
    print("ERROR:")
    traceback.print_exc()
